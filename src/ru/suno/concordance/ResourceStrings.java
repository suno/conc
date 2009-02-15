package ru.suno.concordance;

import java.net.URL;
import java.util.Properties;

/**
 * This is as primary place where all string resources stored.
 * */
public class ResourceStrings {    
    private static final String PROP_FILE = "ru/suno/strings/concordancer.properties";

    // Строки в Диалогах
    public String SELECT_SOURCE_FILE = null;
    public String CREATE_CONCORDANCE = null;
    public String CREATE_SLOVOFORMCOUNTER = null;
    public String CONCORDANCE_DIALOGTITLE = null;
    public String WORD_FILES = null;
    
    // Error messages here
    public String NOT_A_DIRECTORY = null;
    
    public String ERROR_PROBLEM_WITH_PAGES_PARSER = null;
    public String ERROR_GENERAL = null;

    private static ResourceStrings resStrings = null;
    
    public static ResourceStrings getInstance() {
        if (resStrings == null) {
            resStrings = new ResourceStrings();
        }
        return resStrings;
    }
    
    private ResourceStrings() {
        try {
            Properties properties = new Properties();
            URL url = ClassLoader.getSystemResource(PROP_FILE);   
            properties.load(url.openStream());
            // сообщения об ошибках            
            NOT_A_DIRECTORY = properties.getProperty("NOT_A_DIRECTORY");
            
            // строки в диалогах
            SELECT_SOURCE_FILE = properties.getProperty("SELECT_SOURCE_FILE");
            CREATE_CONCORDANCE = properties.getProperty("CREATE_CONCORDANCE");
            CONCORDANCE_DIALOGTITLE = properties.getProperty("CONCORDANCE_DIALOGTITLE");
            WORD_FILES = properties.getProperty("WORD_FILES");
            CREATE_SLOVOFORMCOUNTER = properties.getProperty("CREATE_SLOVOFORMCOUNTER");
            
            ERROR_PROBLEM_WITH_PAGES_PARSER = properties.getProperty("ERROR_PROBLEM_WITH_PAGES_PARSER");
            ERROR_GENERAL = properties.getProperty("ERROR_GENERAL");

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
