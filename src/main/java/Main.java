import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

public class Main {
    private static Logger logger = Logger.getLogger(Main.class);
    public static void main(String[] args) throws Exception {
        ToolRunner.run(new Configuration(), new WordCount(), args);
        ToolRunner.run(new Configuration(), new InMapperWordCount(), args);
        ToolRunner.run(new Configuration(), new IPAverage(), args);
        ToolRunner.run(new Configuration(), new InMapperIPAverage(), args);
        ToolRunner.run(new Configuration(), new PairCrystal(), args);
        ToolRunner.run(new Configuration(), new StripeCrystal(), args);
        ToolRunner.run(new Configuration(), new HybridCrystal(), args);
    }
}
