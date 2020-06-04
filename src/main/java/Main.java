import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: Wordcount <input> <output>");
            System.exit(1);
        }
        ToolRunner.run(new Configuration(), new WordCount("wordcount"), args);
        ToolRunner.run(new Configuration(), new InMapperWordCount("inmapperwordcount"), args);
        ToolRunner.run(new Configuration(), new IPAverage("ipaverage"), args);
        ToolRunner.run(new Configuration(), new InMapperIPAverage("inmapperipaverage"), args);
    }
}
