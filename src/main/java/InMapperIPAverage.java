import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import utils.AverageWriter;
import utils.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InMapperIPAverage extends Configured implements Tool {
    private final String jobName;

    public InMapperIPAverage(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public int run(String[] strings) throws Exception {
        Job job = new Job(getConf());
        job.setJarByClass(InMapperIPAverage.class);
        job.setJobName(jobName);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(AverageWriter.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setMapperClass(MyMapper.class);
        job.setReducerClass(Reduce.class);

        String inputPath = strings[0] + "/" + jobName;
        String outputPath = strings[1] + "/" + jobName;
        FileInputFormat.setInputPaths(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static class MyMapper extends
            Mapper<Object, Text, Text, AverageWriter> {
        private static final AverageWriter AVERAGE_WRITER = new AverageWriter();
        private final Text word = new Text();
        private Map<String, Pair<Long, Integer>> cache;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            super.setup(context);
            cache = new HashMap<>();
        }

        @Override
        protected void map(Object key, Text value, Context context) {
            String[] tokens = value.toString().split(" ");
            String quantity = tokens[tokens.length - 1];
            if (!quantity.equals("-")) {
                Pair<Long, Integer> pair = new Pair<>(Long.parseLong(quantity), 1);
                String ip = tokens[0];
                if (cache.containsKey(ip)) {
                    cache.put(ip, addPair(pair, cache.get(ip)));
                } else {
                    cache.put(ip, pair);
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            for (String s : cache.keySet()) {
                word.set(s);
                Pair<Long, Integer> pair = cache.get(s);
                AVERAGE_WRITER.set(pair.getKey(), pair.getValue());
                context.write(word, AVERAGE_WRITER);
            }
            super.cleanup(context);
        }

        private Pair<Long, Integer> addPair(Pair<Long, Integer> p1, Pair<Long, Integer> p2) {
            long key = p1.getKey() + p2.getKey();
            int value = p1.getValue() + p2.getValue();
            return new Pair<>(key, value);
        }
    }

    public static class Reduce extends
            Reducer<Text, AverageWriter, Text, DoubleWritable> {
        private final DoubleWritable average = new DoubleWritable();

        @Override
        protected void reduce(Text key, Iterable<AverageWriter> values,
                              Context context) throws IOException, InterruptedException {
            long sum = 0;
            int cnt = 0;
            for (AverageWriter value : values) {
                sum += value.getSum();
                cnt += value.getCount();
            }
            average.set(sum / (double) cnt);
            context.write(key, average);
        }
    }
}
