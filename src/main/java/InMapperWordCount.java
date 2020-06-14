import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import static utils.Util.getInputFilePath;
import static utils.Util.getOutputFilePath;

public class InMapperWordCount extends Configured implements Tool {
    private final String jobName;

    public InMapperWordCount() {
        String className = this.getClass().getSimpleName();
        this.jobName = className.toLowerCase();
    }

    @Override
    public int run(String[] strings) throws Exception {
        Job job = new Job(getConf());
        job.setJarByClass(InMapperWordCount.class);
        job.setJobName(jobName);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);

        FileInputFormat.setInputPaths(job, new Path(getInputFilePath(jobName)));
        FileOutputFormat.setOutputPath(job, new Path(getOutputFilePath(jobName)));

        return job.waitForCompletion(false) ? 0 : 1;
    }

    public static class MyMapper extends
            Mapper<Object, Text, Text, IntWritable> {
        private static final IntWritable COUNT = new IntWritable(1);
        private final Logger logger = Logger.getLogger(MyMapper.class);
        private final Text word = new Text();
        private Map<String, Integer> cache;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            super.setup(context);
            cache = new HashMap<>();
        }

        @Override
        protected void map(Object key, Text value, Context context) {
            StringTokenizer tokenizer = new StringTokenizer(value.toString());
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (cache.containsKey(token)) {
                    int p = cache.get(token) + 1;
                    cache.put(token, p);
                } else {
                    cache.put(token, 1);
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            for (String s : cache.keySet()) {
                word.set(s);
                COUNT.set(cache.get(s));
                context.write(word, COUNT);
            }
            super.cleanup(context);
        }
    }

    public static class MyReducer extends
            Reducer<Text, IntWritable, Text, IntWritable> {
        private final IntWritable count = new IntWritable();

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values,
                              Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            count.set(sum);
            context.write(key, count);
        }
    }
}
