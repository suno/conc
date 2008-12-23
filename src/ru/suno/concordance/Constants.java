package ru.suno.concordance;

public class Constants {
    public final static String SLASH = System.getProperty("file.separator");
    public final static String SAME_DIR = ".";
    public final static String PARENT_DIR = "..";

    public final static String WORD_ELEMENT = "word";
    public final static String DOC_ROOT = "root";
    public final static String FULL_WORD = "fullname";
    public final static String COUNT = "count";
    
    private static Constants consts = null;
    
    private Constants() {        
    }
    
    public static Constants getInstance() {
        if (consts == null) {
            consts = new Constants();
        }
        return consts;
    }
}
