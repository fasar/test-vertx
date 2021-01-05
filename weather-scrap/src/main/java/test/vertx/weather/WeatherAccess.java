package test.vertx.weather;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

import static java.time.temporal.ChronoField.*;

public class WeatherAccess {
    static final DateTimeFormatter DATE_FORMATTER;

    static {
        DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 1)
            .appendLiteral('-')
            .appendValue(DAY_OF_MONTH, 1)
            .toFormatter();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        System.setProperty("webdriver.gecko.driver", "/usr/bin/geckodriver");
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        WebDriver driver = new ChromeDriver(options);

        File file = new File("pageSource.html");
        String pageSource = getWeather(driver, "LFLP", LocalDate.of(2021, 1, 1));
        FileUtils.writeStringToFile(file, pageSource, StandardCharsets.UTF_8);
        driver.quit();
    }

    private static String getWeather(WebDriver driver, String city, LocalDate date) throws InterruptedException {
        String dateStr = date.format(DATE_FORMATTER);
        String url = String.format("https://www.wunderground.com/history/daily/%s/date/%s", city, dateStr);
        driver.get(url);
        Thread.sleep(1000);
        String pageSource = driver.getPageSource();
        return pageSource;
    }
}
