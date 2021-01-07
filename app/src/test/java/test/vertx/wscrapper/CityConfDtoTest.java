package test.vertx.wscrapper;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class CityConfDtoTest {

    @Test
    void JsonDecode() throws IOException {
        File file = new File("/home/sartor/work/Sandbox/Java/test-vertx/app/src/main/resources/conf/config.json");
        String s = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        JsonObject config = new JsonObject(s);
        JsonObject weatherScrapConf = config.getJsonObject("weather-scrapper");
        JsonArray citiesJson = weatherScrapConf.getJsonArray("cities");
        CityConfDto[] cityConfDtos = Json.decodeValue(citiesJson.toBuffer(), CityConfDto[].class);
        for (CityConfDto cityConfDto : cityConfDtos) {
            System.out.println(" " + cityConfDto);
        }
    }
}
