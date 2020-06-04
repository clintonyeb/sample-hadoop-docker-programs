package utils;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class AverageWriter implements Writable {
    private long sum = 0;
    private int count = 1;

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeLong(sum);
        dataOutput.writeInt(count);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        sum = dataInput.readLong();
        count = dataInput.readInt();
    }

    public void set(long sum, int count) {
        this.sum = sum;
        this.count = count;
    }

    public long getSum() {
        return sum;
    }

    public void setSum(long sum) {
        this.sum = sum;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
