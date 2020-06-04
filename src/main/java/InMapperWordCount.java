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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class InMapperWordCount extends Configured implements Tool {
    private final String jobName;

    public InMapperWordCount(String jobName) {
        this.jobName = jobName;
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

        String inputPath = strings[0]+ "/" + jobName;
        String outputPath = strings[1] + "/" + jobName;
        FileInputFormat.setInputPaths(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(false) ? 0 : 1;
    }

    public static class MyMapper extends
            Mapper<Object, Text, Text, IntWritable> {
        private static final IntWritable ONE = new IntWritable(1);
        private final Text word = new Text();
        private final Map<Text, IntWritable> cache = new HashMap<>();

        @Override
        protected void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {

            StringTokenizer tokenizer = new StringTokenizer(value.toString());
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                word.set(token);
                if(cache.containsKey(word)) {
                    int p = cache.get(word).get();
                    p = p + ONE.get();
                    cache.put(word, new IntWritable(p));
                } else {
                    cache.put(word, ONE);
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            super.cleanup(context);
            for (Text o : cache.keySet()) {
                context.write(o, cache.get(o));
            }
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
