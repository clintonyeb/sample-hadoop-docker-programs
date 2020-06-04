import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import utils.PairWriter;

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

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(PairWriter.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        String inputPath = strings[0] + "/" + jobName;
        String outputPath = strings[1] + "/" + jobName;
        FileInputFormat.setInputPaths(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(false) ? 0 : 1;
    }

    public static class Map extends
            Mapper<Object, Text, Text, PairWriter> {
        private static final PairWriter pairWriter = new PairWriter();
        private final Text word = new Text();

        @Override
        protected void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] tokens = value.toString().split(" ");
            String quant = tokens[tokens.length - 1];
            if (!quant.equals("-")) {
                word.set(tokens[0]);
                pairWriter.setSum(Long.parseLong(quant));
                pairWriter.setCount(1);
                context.write(word, pairWriter);
            }
        }
    }

    public static class Reduce extends
            Reducer<Text, PairWriter, Text, DoubleWritable> {
        private final DoubleWritable average = new DoubleWritable();

        @Override
        protected void reduce(Text key, Iterable<PairWriter> values,
                              Context context) throws IOException, InterruptedException {
            long sum = 0;
            int cnt = 0;
            for (PairWriter value : values) {
                sum += value.getSum();
                cnt += value.getCount();
            }
            average.set(sum / (double) cnt);
            context.write(key, average);
        }
    }
}