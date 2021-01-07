package test.vertx.utilities;

import org.apache.commons.lang3.StringUtils;
import test.vertx.tsdb.TsDbRecord;
import test.vertx.tsdb.TsDbUnit;

import java.util.List;
import java.util.stream.Collectors;

public class Warp10Utils {

    /**
     * Convert a TsDbRecord in Warp10 compatible ingestion GTS
     * @param record
     * @return
     */
    public static String convertTsDbRecordToWSGTS(TsDbRecord record) {
        List<String> data = record.getData();
        if (data.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String signalName = record.getSignalName();
        long l = record.getStartInstant().toEpochMilli();
        sb.append(l * 1000);
        sb.append("// ");
        sb.append(record.getSignalName());
        String labels = null;
        if (record.getLabels() != null) {
            labels = record.getLabels().stream().map(lab -> lab.getKey() + "=" + lab.getValue()).collect(Collectors.joining(",", "{", "}"));
        }
        if (StringUtils.isBlank(labels)) {
            sb.append("{}");
        } else {
            sb.append(labels);
        }
        sb.append(" ");
        sb.append(toUnint(data.get(0), record.getUnit()));
        sb.append("\n");
        for (int i = 1; i < data.size(); i++) {
            l = l + record.getStepDuration().toMillis();
            sb.append("=");
            sb.append(l * 1000);
            sb.append("// ");
            sb.append(toUnint(data.get(i), record.getUnit()));
            sb.append("\n");
        }
        return sb.toString();
    }


    static String toUnint(String data, TsDbUnit unit) {
        switch (unit) {
            case STRING:
                return "'" + data.replace("'", "\\'") + "'";
            case NUMBER:
            case BOOLEAN:
            default:
                return data;
        }
    }

}
