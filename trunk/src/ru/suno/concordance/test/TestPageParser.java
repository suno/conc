package ru.suno.concordance.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import junit.framework.TestCase;
import ru.suno.concordance.ConcPage;
import ru.suno.concordance.parser.PagesParser;
import ru.suno.concordance.utils.IWordFile;
import ru.suno.concordance.utils.InputFileFactory;

public class TestPageParser extends TestCase {

	public static final String CONCORDANCE_TESTDATA = "/home/suno/eclipse_workspace/Concordance/testdata/page_parser_test.doc";

	public void testPageParser() {
        log("Чтение текста из исходных файлов...");
        // прочесть текст в буфер из всех файлов        
        IWordFile wr = InputFileFactory.getWordFile();
        String allText = "";
        String fileText = wr.readWordFileContent(new File(CONCORDANCE_TESTDATA));
        if (fileText != null) {
            allText += fileText;
        }
        log("Исходный текст загружен!");
        
        log("Разбиение текста на страницы...");
        // разбить ВЕСЬ текст на страницы
        PagesParser pp = new PagesParser();
        
        Collection<ConcPage> allPages = null;
		try {
			allPages = pp.parseTextToPages(allText);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        log("Разбиение текста на страницы завершено!");
        
        FileWriter fileOut = null; 
        try {
        	fileOut = new FileWriter("/home/suno/eclipse_workspace/Concordance/testdata/result.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
        
        
        for (ConcPage page : allPages) {
        	try {
				fileOut.write("\n" + page.getText());
				fileOut.write("*** *** ***\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        }
        
        try {
			fileOut.flush();
			fileOut.close();			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
	}
	
    private void log(String message) {
    	System.out.println(message);
    }
}
