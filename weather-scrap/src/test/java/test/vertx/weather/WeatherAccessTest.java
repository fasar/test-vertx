package test.vertx.weather;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static test.vertx.weather.WeatherAccess.DATE_FORMATTER;

class WeatherAccessTest {

    @Test
    void testDateFormatter() {
        String format = LocalDate.of(2020, 1, 1).format(DATE_FORMATTER);
        Assertions.assertEquals(8, format.length());
    }

}
