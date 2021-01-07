package test.vertx.wscrapper;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.vertx.tsdb.TsDbLabel;
import test.vertx.tsdb.TsDbRecord;
import test.vertx.utils.VertxUtils;
import test.vertx.weather.WeatherAccess;
import test.vertx.weather.WeatherExtract;
import test.vertx.weather.WeatherSignal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherScrapperApp {
    private static Logger LOG = LoggerFactory.getLogger(WeatherScrapperApp.class);
    private static Random ran = new Random();

    public static void main(String[] args) {
        // Configure jackson
        JavaTimeModule module = new JavaTimeModule();
        DatabindCodec.mapper().registerModule(module);
        DatabindCodec.prettyMapper().registerModule(module);


        Vertx vertx = Vertx.vertx();
        ConfigRetriever retriever = VertxUtils.loadConfiguration(vertx);
        retriever.configStream()
            .handler(conf -> {
                startVerticles(vertx, conf);

                Instant lastDateInLogs = null;
                try {
                    lastDateInLogs = getLastDateInLogs(conf);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
                Instant downloadInstant;
                if (lastDateInLogs == null) {
                    downloadInstant = Instant.now().minus(1, ChronoUnit.DAYS);
                } else {
                    downloadInstant = lastDateInLogs;
                }
                downloadInstant = LocalDate.of(2021, 1, 1).atStartOfDay(ZoneId.of("Europe/Paris")).toInstant();
                startScrapAndSave(vertx, conf, downloadInstant);
            });

    }

    private static void startScrapAndSave(Vertx vertx, JsonObject conf, Instant downloadInstant) {
        Thread scrapAndSave = new Thread(() -> {
            try {
                scrapAndSaveFunction(vertx, conf, downloadInstant);
                LOG.info("Application finish to scrap weather");
                vertx.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });
        scrapAndSave.start();
    }

    private static void scrapAndSaveFunction(Vertx vertx, JsonObject conf, Instant downloadInstant) throws IOException {
        JsonObject jsonConf = conf.getJsonObject("weather-scrapper");
        JsonArray citiesJson = jsonConf.getJsonArray("cities");

        CityConfDto[] cityConfDtos = Json.decodeValue(citiesJson.toBuffer(), CityConfDto[].class);
        WeatherAccess weatherAccess = new WeatherAccess();
        Instant now = Instant.now();
        for (CityConfDto city : cityConfDtos) {
            ZoneId zoneId = ZoneId.of(city.getZoneId());
            ZonedDateTime zonedDateTime = downloadInstant.atZone(zoneId);
            LocalDate dateToDl = zonedDateTime.toLocalDate();
            LocalDate nowDate = now.atZone(zoneId).toLocalDate();
            int nbDays = jsonConf.getInteger("nb-days-to-scrap");
            while (dateToDl.isBefore(nowDate) && nbDays > 0) {
                LOG.info("Get weather for {} at {} for {}", city.getName(), dateToDl, zoneId);
                try {
                    // Get the Weather HTML page on WUnderground
                    String weatherHtml = getWeatherHtmlPage(jsonConf, weatherAccess, city, dateToDl);
                    // Extract the Weather from the page
                    WeatherExtract weatherExtract = new WeatherExtract(weatherHtml);
                    Instant startInstantOfSignal = dateToDl.atStartOfDay(zoneId).toInstant();
                    List<WeatherSignal> signals = weatherExtract.convert(startInstantOfSignal);
                    // Check the city is the good city
                    boolean isCity = weatherExtract.titleContains(city.getName()) || weatherExtract.titleContains(city.getHtmlTitleKey()) ;
                    if (!isCity) {
                        LOG.error("The document fetched for {} at {} do not container the name of the city in the title", city.getName(), dateToDl);
                    } else {
                        // Create the label for the signal
                        List<TsDbLabel> labels = new ArrayList<>();
                        labels.add(new TsDbLabel("city", city.getName()));
                        for (WeatherSignal weatherSignal : signals) {
                            // Convert the weather in the format for the verticles
                            List<TsDbLabel> label2s = new ArrayList<>();
                            label2s.addAll(labels);
                            label2s.add(new TsDbLabel("observation", weatherSignal.getObservationName()));
                            TsDbRecord convert = WeatherScrapperUtils.convert("weather.wunderground", label2s, weatherSignal);
                            vertx.eventBus().publish("tsdb.data", Json.encode(convert));
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Can't get weather for {} at {}. {}", city.getName(), dateToDl, e.getMessage(), e);
                }

                dateToDl = dateToDl.plusDays(1);
                nbDays--;
            }
        }
        weatherAccess.closeDriver();

    }

    private static String getWeatherHtmlPage(JsonObject jsonConf, WeatherAccess weatherAccess, CityConfDto city, LocalDate dateToDl) throws InterruptedException {
        String outputPath = jsonConf.getString("html-scraped-folder");
        File outputFolder = new File(outputPath);
        outputFolder.mkdirs();
        String fileName = city.getName() + "-" + dateToDl.toString() + ".html";
        File htmlFile = new File(outputFolder, fileName);
        String htmlContent = null;
        if (htmlFile.exists()) {
            try {
                htmlContent = FileUtils.readFileToString(htmlFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        if (htmlContent == null) {
            htmlContent = weatherAccess.getWeather(city.getAnagram(), dateToDl);
            // Wait to not flood the server !
            try {
                int waiting = 2000 + ran.nextInt(5) * 1000;
                LOG.info("Waiting {}", Duration.ofMillis(waiting));
                Thread.sleep(waiting);
            } catch (InterruptedException e) {
                // Do not care
            }
            if (htmlContent != null) {
                try {
                    FileUtils.write(htmlFile, htmlContent, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        return htmlContent;
    }

    /**
     * Read gts log file to find the last TimeStamps inserted readed in
     *
     * @param conf
     * @return
     * @throws IOException
     */
    private static Instant getLastDateInLogs(JsonObject conf) throws IOException {
        String tsdbFile = conf.getJsonObject("tsdb-file").getString("output-file");
        File file = new File(tsdbFile);
        if (!file.exists()) {
            LOG.info("Files {} does not exists", tsdbFile);
            return null;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            long endOfFile = file.length() - 1000;
            fileInputStream.skip(endOfFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            Pattern pattern = Pattern.compile(".?([\\d]+)[^\\d]");
            Long maxTs = bufferedReader.lines().skip(1)
                .reduce(0L, (acc, line) -> {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String group = matcher.group(1);
                        try {
                            long l = Long.parseLong(group);
                            return Math.max(acc, l);
                        } catch (Exception e) {
                            // Can't convert, do not care
                        }
                    }
                    return acc;
                }, Math::max);
            if (maxTs == 0) {
                return null;
            }
            Instant lastInstant = Instant.ofEpochMilli(maxTs);
            return lastInstant;
        } catch (Exception e) {
            LOG.error("Can't open tsdb files {}", tsdbFile);
        }
        return null;
    }

    private static void startVerticles(Vertx vertx, JsonObject config) {
        // Deploy HTTP Verticle
        JsonObject warp10Config = config.getJsonObject("warp10");
        vertx
            .deployVerticle("test.vertx.tsdb.Warp10Verticle", new DeploymentOptions().setConfig(warp10Config))
            .onSuccess(verticleId -> {
                System.out.println("Warp10 Verticle deployed with id : " + verticleId);
            }).onFailure(e -> e.printStackTrace())
        ;
        JsonObject fileTsdbConf = config.getJsonObject("tsdb-file");
        vertx
            .deployVerticle("test.vertx.tsdb.FileTsDbVerticle", new DeploymentOptions().setConfig(fileTsdbConf))
            .onSuccess(verticleId -> {
                System.out.println("FileTsDb Verticle deployed with id : " + verticleId);
            }).onFailure(e -> e.printStackTrace())
        ;
    }
}
