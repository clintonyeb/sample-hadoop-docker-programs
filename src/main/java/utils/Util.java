package utils;

public abstract class Util {
    public static String getInputFilePath(String path) {
        return "file:///input/" + path;
    }

    public static String getOutputFilePath(String path) {
        return "file:///output/" + path;
    }
}
