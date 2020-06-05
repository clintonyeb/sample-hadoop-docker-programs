import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.log4j.Logger;
import utils.CrystalWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HybridCrystal extends Configured implements Tool {
//    private static final Logger logger = Logger.getLogger(HybridCrystal.class);
    private final String jobName;

    public HybridCrystal() {
        String className = this.getClass().getSimpleName();
        this.jobName = className.toLowerCase();
    }

    @Override
    public int run(String[] strings) throws Exception {
        Job job = new Job(getConf());
        job.setJarByClass(HybridCrystal.class);
        job.setJobName(jobName);

        job.setMapOutputKeyClass(CrystalWriter.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(utils.MapWritable.class);

        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        job.setPartitionerClass(MyPartitioner.class);

        String inputPath = strings[0] + "/" + jobName;
        String outputPath = strings[1] + "/" + jobName;
        FileInputFormat.setInputPaths(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

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
            }
        }
    }

    public static class MyReducer extends
            Reducer<CrystalWriter, IntWritable, Text, utils.MapWritable> {
        private Text currentKey = null;
        private final Map<String, Integer> mapWritable = new HashMap<>();

        @Override
        protected void reduce(CrystalWriter key, Iterable<IntWritable> values,
                              Context context) throws IOException, InterruptedException {
            if(currentKey == null) {
                currentKey = new Text();
            } else if(!key.getU().equals(currentKey)) {
                processMap(context);
            }
            String curr = key.getV().toString();
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            mapWritable.put(curr, sum);
            currentKey.set(key.getU());
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            processMap(context);
            super.cleanup(context);
        }

        private void processMap(Context context) throws IOException, InterruptedException {
            int s = sumMap(mapWritable);
            utils.MapWritable results = divideMap(mapWritable, s);
            context.write(currentKey, results);
            mapWritable.clear();
        }

        private void addMapValue(Map<String, Integer> m, String key, int v) {
            int value = m.get(key) + v;
            m.put(key, value);
        }

        private int sumMap(Map<String, Integer> map) {
            int sum = 0;
            int curr;
            for (String k : map.keySet()) {
                curr = map.get(k);
                sum += curr;
            }
            return sum;
        }

        private utils.MapWritable divideMap(Map<String, Integer> map, int value) {
            int curr;
            utils.MapWritable res = new utils.MapWritable();
            for (String k : map.keySet()) {
                curr = map.get(k);
                res.put(new Text(k), new DoubleWritable(curr / (double) value));
            }
            return res;
        }
    }

    public static class MyPartitioner extends Partitioner<CrystalWriter, IntWritable> {
        @Override
        public int getPartition(CrystalWriter crystalWriter, IntWritable intWritable, int i) {
            return Math.abs(crystalWriter.getU().hashCode()) % i;
        }
    }
}
