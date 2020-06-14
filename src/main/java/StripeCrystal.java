import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.log4j.Logger;

import java.io.IOException;

import static utils.Util.getInputFilePath;
import static utils.Util.getOutputFilePath;

public class StripeCrystal extends Configured implements Tool {
    private final String jobName;

    public StripeCrystal() {
        String className = this.getClass().getSimpleName();
        this.jobName = className.toLowerCase();
    }

    @Override
    public int run(String[] strings) throws Exception {
        Job job = new Job(getConf());
        job.setJarByClass(StripeCrystal.class);
        job.setJobName(jobName);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(utils.MapWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(utils.MapWritable.class);

        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);

        FileInputFormat.setInputPaths(job, new Path(getInputFilePath(jobName)));
        FileOutputFormat.setOutputPath(job, new Path(getOutputFilePath(jobName)));

        return job.waitForCompletion(false) ? 0 : 1;
    }

    public static class MyMapper extends
            Mapper<Object, Text, Text, utils.MapWritable> {
        private static final IntWritable ONE = new IntWritable(1);
        private final Text word = new Text();
        private final utils.MapWritable mapWritable = new utils.MapWritable();


        @Override
        protected void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] tokens = value.toString().split(" ");
            for (int i = 0; i < tokens.length; i++) {
                mapWritable.clear();
                String token = tokens[i];
                processWindow(i, tokens, mapWritable);
                word.set(token);
                context.write(word, mapWritable);
            }
        }

        private void processWindow(int position, String[] record, utils.MapWritable map) {
            String elem = record[position];
            while (++position < record.length && !elem.equals(record[position])) {
                Text curr = new Text(record[position]);
                if (map.containsKey(curr)) {
                    incrementMapValue(map, curr);
                } else {
                    map.put(curr, ONE);
                }
            }
        }

        private void incrementMapValue(utils.MapWritable m, Text key) {
            int value = ((IntWritable) m.get(key)).get() + 1;
            m.put(key, new IntWritable(value));
        }
    }

    public static class MyReducer extends
            Reducer<Text, utils.MapWritable, Text, utils.MapWritable> {
        @Override
        protected void reduce(Text key, Iterable<utils.MapWritable> values,
                              Context context) throws IOException, InterruptedException {
            int s;
            utils.MapWritable finalMap = new utils.MapWritable();
            for (utils.MapWritable writable : values) {
                addMaps(finalMap, writable);
            }
            s = sumMap(finalMap);
            divideMap(finalMap, s);
            context.write(key, finalMap);
        }


        private void addMaps(utils.MapWritable m1, utils.MapWritable m2) {
            for (Writable k : m2.keySet()) {
                Text key = (Text) k;
                if (m1.containsKey(key)) {
                    addMapValue(m1, key, (IntWritable) m2.get(key));
                } else {
                    m1.put(key, m2.get(key));
                }
            }
        }

        private int sumMap(utils.MapWritable map) {
            int sum = 0;
            int curr;
            for (Writable k : map.keySet()) {
                curr = ((IntWritable) map.get(k)).get();
                sum += curr;
            }
            return sum;
        }

        private void divideMap(utils.MapWritable map, int value) {
            int curr;
            for (Writable k : map.keySet()) {
                curr = ((IntWritable) map.get(k)).get();
                map.put(k, new DoubleWritable(curr / (double) value));
            }
        }

        private void addMapValue(utils.MapWritable m, Text key, IntWritable v) {
            int value = ((IntWritable) m.get(key)).get() + v.get();
            m.put(key, new IntWritable(value));
        }
    }
}
