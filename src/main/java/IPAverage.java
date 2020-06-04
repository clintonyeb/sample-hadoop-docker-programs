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

public class IPAverage extends Configured implements Tool {
    private final String jobName;

    public IPAverage(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public int run(String[] strings) throws Exception {
        Job job = new Job(getConf());
        job.setJarByClass(IPAverage.class);
        job.setJobName(jobName);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        String inputPath =  strings[0] + "/" + jobName;
        String outputPath = strings[1] + "/" + jobName;
        FileInputFormat.setInputPaths(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(false) ? 0 : 1;
    }

    public static class Map extends
            Mapper<Object, Text, Text, IntWritable> {
        private static final IntWritable quantity = new IntWritable();
        private final Text word = new Text();

        @Override
        protected void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] tokens = value.toString().split(" ");
            word.set(tokens[0]);
            quantity.set(Integer.parseInt(tokens[tokens.length-1]));
            context.write(word, quantity);
        }
    }

    public static class Reduce extends
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
