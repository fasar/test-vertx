package test.vertx.wscrapper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class CityConfDto {
    String name;
    String anagram;
    String zoneId;
    String htmlTitleKey;
}
