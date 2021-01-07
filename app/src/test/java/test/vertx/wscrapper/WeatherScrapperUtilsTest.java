package test.vertx.wscrapper;

import org.junit.jupiter.api.Test;
import test.vertx.tsdb.TsDbLabel;
import test.vertx.tsdb.TsDbRecord;
import test.vertx.weather.WeatherExtract;
import test.vertx.weather.WeatherSignal;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

class WeatherScrapperUtilsTest {

    @Test
    void testConvert() throws IOException {
        WeatherExtract weatherExtract = new WeatherExtract(new File("/home/sartor/work/Sandbox/Java/test-vertx/weather-scrap/src/test/resources/test/vertx/weather/pageSource20210101.html"));
        ZonedDateTime startDate = LocalDateTime.parse("2020-01-01T00:00:00").atZone(ZoneId.of("Europe/Paris"));
        List<WeatherSignal> converted = weatherExtract.convert(startDate.toInstant());
        List<TsDbLabel> labels = new ArrayList<>();
        labels.add(new TsDbLabel("city", "Annecy"));
        for (WeatherSignal weatherSignal : converted) {
            List<TsDbLabel> label2s = new ArrayList<>();
            label2s.addAll(labels);
            label2s.add(new TsDbLabel("observation", weatherSignal.getObservationName()));
            TsDbRecord convert = WeatherScrapperUtils.convert("weather.wunderground", label2s, weatherSignal);
            System.out.println("Converted for " + weatherSignal.getObservationName());
            System.out.println(convert);
        }
    }
}
