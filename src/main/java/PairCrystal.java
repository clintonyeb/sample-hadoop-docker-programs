import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import utils.CrystalWriter;

import java.io.IOException;

import static utils.Util.getInputFilePath;
import static utils.Util.getOutputFilePath;

public class PairCrystal extends Configured implements Tool {
    private final String jobName;

    public PairCrystal() {
        String className = this.getClass().getSimpleName();
        this.jobName = className.toLowerCase();
    }

    @Override
    public int run(String[] strings) throws Exception {
        Job job = new Job(getConf());
        job.setJarByClass(PairCrystal.class);
        job.setJobName(jobName);

        job.setMapOutputKeyClass(CrystalWriter.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(CrystalWriter.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        job.setPartitionerClass(MyPartitioner.class);

        FileInputFormat.setInputPaths(job, new Path(getInputFilePath(jobName)));
        FileOutputFormat.setOutputPath(job, new Path(getOutputFilePath(jobName)));

        return job.waitForCompletion(false) ? 0 : 1;
    }

    public static class MyMapper extends
            Mapper<Object, Text, CrystalWriter, IntWritable> {
        private static final IntWritable ONE = new IntWritable(1);
        private static final CrystalWriter pair = new CrystalWriter();
        private final Text u = new Text();
        private final Text v = new Text();

        @Override
        protected void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] tokens = value.toString().split(" ");
            for (int i = 0; i < tokens.length; i++) {
                processWindow(i, tokens, context);
            }
        }

        private void processWindow(int position, String[] record, Context context) throws IOException, InterruptedException {
            String elem = record[position];
            while (++position < record.length && !elem.equals(record[position])) {
                u.set(elem);
                v.set(record[position]);
                pair.set(u, v);
                context.write(pair, ONE);
                v.set("*");
                context.write(pair, ONE);
            }
        }
    }

    public static class MyReducer extends
            Reducer<CrystalWriter, IntWritable, CrystalWriter, DoubleWritable> {
        private final DoubleWritable avg = new DoubleWritable();
        private long sum = 0;

        @Override
        protected void reduce(CrystalWriter key, Iterable<IntWritable> values,
                              Context context) throws IOException, InterruptedException {
            int s = 0;
            for (IntWritable value : values) {
                s += value.get();
            }
            if (key.getV().toString().equals("*")) {
                sum = s;
            } else {
                avg.set(s / (double) sum);
                context.write(key, avg);
            }
        }
    }

    public static class MyPartitioner extends Partitioner<CrystalWriter, IntWritable> {
        @Override
        public int getPartition(CrystalWriter crystalWriter, IntWritable intWritable, int i) {
            return Math.abs(crystalWriter.getU().hashCode()) % i;
        }
    }
}
