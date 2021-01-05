package test.vertx.weather;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static test.vertx.weather.WeatherExtract.convertData;
import static test.vertx.weather.WeatherExtract.extractData;

class WeatherExtractTest {
    @Test
    void testExtractWeather1() throws IOException {
        File in = new File("src/test/resources/test/vertx/weather/pageSource20210104.html");
        Document doc = Jsoup.parse(in, "UTF8");
        List<List<String>> res = extractData(doc);
        Assertions.assertEquals(30, res.size());
    }

    @Test
    void testExtractWeather2() throws IOException {
        File in = new File("src/test/resources/test/vertx/weather/pageSource20210101.html");
        Document doc = Jsoup.parse(in, "UTF8");
        List<List<String>> res = extractData(doc);
        Assertions.assertEquals(49, res.size());

        List<WeatherSignal> weatherSignals = convertData(res, Instant.parse("2021-01-01T00:00:00.000Z"));
        Assertions.assertEquals(9,weatherSignals.size());
        Assertions.assertEquals(48,weatherSignals.get(0).getData().size());

    }


}
