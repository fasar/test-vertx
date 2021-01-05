package test.vertx.weather;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class WeatherAccess {
    public static void main(String[] args) throws InterruptedException, IOException {
        System.setProperty("webdriver.gecko.driver", "/usr/bin/geckodriver");
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        WebDriver driver = new ChromeDriver();
        File file = new File("pageSource.html");
        getWeather(driver, "LFLP", file);
        driver.quit();
    }

    private static void getWeather(WebDriver driver, String city, File file) throws InterruptedException, IOException {
        String url = String.format("https://www.wunderground.com/history/daily/%s/date/2021-1-4", city);
        driver.get(url);
        Thread.sleep(1000);
        String pageSource = driver.getPageSource();
        FileUtils.writeStringToFile(file, pageSource, StandardCharsets.UTF_8);
    }
}
