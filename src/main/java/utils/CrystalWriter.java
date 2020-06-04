package utils;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class CrystalWriter implements WritableComparable<CrystalWriter> {
    private Text u;
    private Text v;

    public CrystalWriter() {
        this.u = new Text();
        this.v = new Text();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        u.write(dataOutput);
        v.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        u.readFields(dataInput);
        v.readFields(dataInput);
    }

    public void set(Text u, Text v) {
        this.u = u;
        this.v = v;
    }

    public Text getU() {
        return u;
    }

    public void setU(Text u) {
        this.u = u;
    }

    public Text getV() {
        return v;
    }

    public void setV(Text v) {
        this.v = v;
    }

    @Override
    public int compareTo(CrystalWriter o) {
        int res = this.u.compareTo(o.u);
        if(res != 0) return res;
        return this.v.compareTo(o.v);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrystalWriter that = (CrystalWriter) o;
        return Objects.equals(u, that.u) &&
                Objects.equals(v, that.v);
    }

    @Override
    public int hashCode() {
        return Objects.hash(u, v);
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", u, v);
    }
}
