package test.vertx.weather;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WeatherExtract {
    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.parse(new File("pageSource.html"), "UTF8");
        List<List<String>> res = extractData(doc);
        System.out.println("The final Is :");
        System.out.println(res);

    }

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
}
