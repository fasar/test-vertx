package test.vertx.wscrapper;

import org.apache.commons.lang3.StringUtils;
import test.vertx.tsdb.TsDbLabel;
import test.vertx.tsdb.TsDbRecord;
import test.vertx.tsdb.TsDbUnit;
import test.vertx.weather.WeatherSignal;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WeatherScrapperUtils {

    public static TsDbRecord convert(String signalName, List<TsDbLabel> lables, WeatherSignal signal) {
        Instant startInstant = signal.startInstant;
        Duration step = signal.getStep();
        TsDbUnit unit = unitOf(signal.getObservationName());
        List<String> data = signal.getData();
        List<String> convertedData = convertData(signal.getObservationName(), data);
        TsDbRecord record = new TsDbRecord(signalName, lables, startInstant, step, unit, convertedData);
        return record;
    }

    private static List<String> convertData(String observationName, List<String> data) {
        Function<String, String> converter;

        if (observationName.equalsIgnoreCase("Temperature")
            || observationName.equalsIgnoreCase("Dew Point")
        ) {
            // Convert from Fahrenheit
            converter = s -> {
                s = firstField(s);
                double res = (Double.parseDouble(s) - 32) * 5 / 9;
                return String.format("%.2f", res);
            };
        } else if (observationName.equalsIgnoreCase("Humidity")) {
            // Convert from percentage
            converter = s -> {
                s = firstField(s);
                double res = (Double.parseDouble(s) / 100);
                return Double.toString(res);
            };
        } else if (observationName.equalsIgnoreCase("Wind")
            || observationName.equalsIgnoreCase("Condition")
        ) {
            // Convert String
            converter = s -> {
                return s;
            };
        } else if (observationName.equalsIgnoreCase("Wind Speed")
            || observationName.equalsIgnoreCase("Wind Gust")
        ) {
            // convert mph in meter / sec
            converter = s -> {
                s = firstField(s);
                double res = (Double.parseDouble(s) * (1.609344 * 1000 / 3600));
                return String.format("%.2f", res);
            };
        } else if (observationName.equalsIgnoreCase("Pressure")) {
            // convert Inch of Mercury in Bar
            converter = s -> {
                s = firstField(s);
                double res = (Double.parseDouble(s) * 33.863886667 / 1000);
                return String.format("%.2f", res);
            };
        } else if (observationName.equalsIgnoreCase("Precip.")) {
            // Convert Inch in meter
            converter = s -> {
                s = firstField(s);
                double res = (Double.parseDouble(s) * 0.0254);
                return String.format("%.2f", res);
            };
        } else {
            throw new IllegalArgumentException("Observation name " + observationName + " not fond");
        }


        List<String> collect = data.stream().map(converter).collect(Collectors.toList());
        return collect;
    }

    private static String firstField(String s) {
        if (StringUtils.isBlank(s)) {
            return "";
        }
        return s.split(" ")[0];
    }

    private static TsDbUnit unitOf(String name) {
        if (name.equalsIgnoreCase("Temperature")) {
            return TsDbUnit.NUMBER;
        } else if (name.equalsIgnoreCase("Dew Point")) {
            return TsDbUnit.NUMBER;
        } else if (name.equalsIgnoreCase("Humidity")) {
            return TsDbUnit.NUMBER;
        } else if (name.equalsIgnoreCase("Wind")) {
            return TsDbUnit.STRING;
        } else if (name.equalsIgnoreCase("Wind Speed")) {
            return TsDbUnit.NUMBER;
        } else if (name.equalsIgnoreCase("Wind Gust")) {
            return TsDbUnit.NUMBER;
        } else if (name.equalsIgnoreCase("Pressure")) {
            return TsDbUnit.NUMBER;
        } else if (name.equalsIgnoreCase("Precip.")) {
            return TsDbUnit.NUMBER;
        } else if (name.equalsIgnoreCase("Condition")) {
            return TsDbUnit.STRING;
        }
        return TsDbUnit.NUMBER;
    }
}
