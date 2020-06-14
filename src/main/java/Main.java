import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;

public class Main {
//    private static Logger logger = Logger.getLogger(Main.class);
    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration();
        ToolRunner.run(config, new WordCount(), args);
        ToolRunner.run(config, new InMapperWordCount(), args);
        ToolRunner.run(config, new IPAverage(), args);
        ToolRunner.run(config, new InMapperIPAverage(), args);
        ToolRunner.run(config, new PairCrystal(), args);
        ToolRunner.run(config, new StripeCrystal(), args);
        ToolRunner.run(config, new HybridCrystal(), args);
    }
}
