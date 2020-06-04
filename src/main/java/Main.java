import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configuration;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: Wordcount <input> <output>");
            System.exit(1);
        }
        int res = ToolRunner.run(new Configuration(), new WordCount(), args);
        if(res != 0) System.exit(res);
        res = ToolRunner.run(new Configuration(), new InMapperWordCount(), args);
        if(res != 0) System.exit(res);
    }
}
