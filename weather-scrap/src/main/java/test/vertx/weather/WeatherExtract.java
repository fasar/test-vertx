package test.vertx.weather;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WeatherExtract {
    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.parse(new File("pageSource.html"), "UTF8");
        List<List<String>> res = extractData(doc);
        List<WeatherSignal> weatherSignals = convertData(res, Instant.parse("2020-01-01T00:00:00Z"));
        System.out.println("The final Is :");
        System.out.println(weatherSignals);

    }

    /**
     * Extract row data from HTML page in the Document argument
     *
     * @param doc
     * @return
     */
    public static List<List<String>> extractData(Document doc) {
        List<List<String>> res = new ArrayList<>();
        Element select = doc.select(".observation-table").first();
        Element table = select.select("table").first();
        Elements headers = table.select("thead th");
        List<String> res1 = new ArrayList<>();
        res.add(res1);
        for (Element header : headers) {
            res1.add(header.text());
        }
        Elements lines = table.select("tbody tr");
        for (Element line : lines) {
            Elements data = line.select("td");
            List<String> res2 = new ArrayList<>();
            res.add(res2);
            for (Element datum : data) {
                res2.add(datum.text());
            }
        }
        return res;
    }

    /**
     * Convert the argument in List of signal.
     * @param data
     * @param startInstant
     * @return
     */
    public static List<WeatherSignal> convertData(List<List<String>> data, Instant startInstant) {
        Objects.requireNonNull(data, "Data should not be null");
        Objects.requireNonNull(startInstant, "Start Instant should not be null");
        if (data.size() < 1) {
            throw new IllegalArgumentException("Data should not be empty");
        }
        int dataSize = data.size();
        List<String> headers = data.get(0);
        int headerSize = headers.size();
        for (int i = 1; i < dataSize; i++) {
            List<String> line = data.get(i);
            if (line.size() != headerSize) {
                throw new IllegalArgumentException("Line of data should have the same element size as header");
            }
        }

        List<WeatherSignal> res = new ArrayList<>();
        for (int i = 1; i < headers.size(); i++) { // Skip the first column, it is the time
            String dataName = headers.get(i);
            ArrayList<String> signal = new ArrayList<>();
            WeatherSignal weatherSignal = new WeatherSignal(dataName, startInstant, Duration.ofMinutes(30), signal);
            res.add(weatherSignal);
            for (int j = 1; j < data.size(); j++) { // Skip the first line, it is the headers
                signal.add(data.get(j).get(i));
            }
        }
        return res;
    }

    /**
     * Convert the signal in
     *
     * @param signalName
     * @param inputs
     */
    public static void convertSignal(String signalName, List<String> inputs) {

    }
}
