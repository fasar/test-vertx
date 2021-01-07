package test.vertx.utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import test.vertx.tsdb.TsDbLabel;
import test.vertx.tsdb.TsDbRecord;
import test.vertx.tsdb.TsDbUnit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class Warp10UtilsTest {

    @Test
    void testConvert() {
        List<String> data = new ArrayList<>();
        data.add("1");
        data.add("2");
        data.add("3");
        List<TsDbLabel> labels = new ArrayList<>();
        labels.add(new TsDbLabel("key", "value"));
        TsDbRecord record = new TsDbRecord("test", labels, Instant.parse("2020-01-01T00:00:00Z"), Duration.ofMinutes(30), TsDbUnit.NUMBER, data);
        String s = Warp10Utils.convertTsDbRecordToWSGTS(record);
        String[] lines = s.split("\n");
        Assertions.assertEquals(3, lines.length);
    }
}
