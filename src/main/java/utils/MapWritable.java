package utils;

import org.apache.hadoop.io.Writable;

public class MapWritable extends org.apache.hadoop.io.MapWritable {
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Writable key : this.keySet()) {
            builder.append(String.format("{%s: %s}  ", key, get(key)));
        }
        return builder.toString();
    }
}
