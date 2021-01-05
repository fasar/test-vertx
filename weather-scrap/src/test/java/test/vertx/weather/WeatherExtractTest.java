package test.vertx.weather;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static test.vertx.weather.WeatherExtract.extractData;

class WeatherExtractTest {
    @Test
    void testExtractWeather() throws IOException {
        File in = new File("src/test/resources/test/vertx/weather/pageSource.html");
        Document doc = Jsoup.parse(in, "UTF8");
        List<List<String>> res = extractData(doc);
        Assertions.assertEquals(30, res.size());
    }
}
