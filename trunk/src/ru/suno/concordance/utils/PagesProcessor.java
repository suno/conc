package ru.suno.concordance.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ru.suno.concordance.ConcPage;
import ru.suno.concordance.ConcSentence;
import ru.suno.concordance.ConcWord;
import ru.suno.concordance.ConcWordList;
import ru.suno.concordance.dlg.FileSelectDialog;
import ru.suno.concordance.parser.PagesParser;
import ru.suno.concordance.parser.SentenceParser;
import ru.suno.concordance.parser.StringFilter;
import ru.suno.concordance.parser.WordParser;

public class PagesProcessor {
    
    //private static final int MAX_CHARS_LIMIT = 65535;
    private String docPath = "";
    private String resultName;
    
    private FileSelectDialog m_fileSelectDialog = null;
    
    public PagesProcessor(FileSelectDialog aFileSelectDialog) {
    	m_fileSelectDialog = aFileSelectDialog;
    }
    
    private void log(String message) {
    	if (m_fileSelectDialog != null) {
    		m_fileSelectDialog.logMessage(message);
    	} else {
    		System.out.println(message);
    	}
    }
    
    // TODO: replace FileSelectDialog argument with action listener
    public String processAllDocuments(List<File> srcList) {
        String result = "";        
        
        log("Чтение текста из исходных файлов...");
        // прочесть текст в буфер из всех файлов        
        IWordFile wr = InputFileFactory.getWordFile();
        String allText = "";
        for (File file : srcList) {
            String fileText = wr.readWordFileContent(file);
            if (fileText != null) {
                allText += fileText;
            }
        }
        log("Исходный текст загружен!");
        
        log("Разбиение текста на страницы...");
        // разбить ВЕСЬ текст на страницы
        PagesParser pp = new PagesParser();
        Collection<ConcPage> allPages = pp.parseTextToPages(allText);
        log("Разбиение текста на страницы завершено!");
        
        log("Разбиение страниц на предложения...");
        // разбить каждую страницу из массива<страницы> на предложения
        Collection<ConcSentence> allSentences = new ArrayList<ConcSentence>();
        SentenceParser sp = new SentenceParser();
        
        ConcSentence lastSentence = null;
        
        for (ConcPage pg : allPages) {
            // получить текст страницы
            String sen = pg.getText();
            
            // распарсить текст на предложения
            Collection<ConcSentence> pgSents = 
                sp.getAllSentencies(sen, pg.getReference(), lastSentence);
            
            // занести результат в общий массив предложений
            if (pgSents != null) { 
                
                Iterator<ConcSentence> it = pgSents.iterator();
                while(it.hasNext()) {
                    lastSentence = it.next();
                }                                
                
                allSentences.addAll(pgSents);
            }
        }
        allPages.clear();
        allPages = null;        
        log("Разбиение страниц на предложения завершено!");
        
        log("Разбиение всего текста на слова...");
        // разбить ВЕСЬ текст на слова
        Collection<ConcWord> allWords = new ConcWordList();
        WordParser wp = new WordParser();
        Collection<ConcWord> wrds = wp.getAllWords(allText);
        log("Разбиение всего текста на слова завершено!");
            	
        log("Поиск слов в тексте...");
		ConcWord countWord = null;
		if (wrds != null) {
			// добавить найденные в тексте слова
			for (ConcWord w : wrds) {
				if (!allWords.contains(w)) {
					// слова нет в списке
					countWord = w;
					countWord.incrementNumberOfOccurences();					
					allWords.add(countWord);					
				} else {
					// слово есть в списке
					countWord.incrementNumberOfOccurences();					
				}
			}
		}
		log("Поиск слов в тексте завершен!");
        
		log("Составление списка предложений в которых есть слово...");
        // для каждого слова из массива <слова>
        for (ConcWord w : allWords) {
            for (ConcSentence s : allSentences) {
                // проверить, входит ли это слово в любое из 
                // предложений в массиве <предложения>
                if (s.getSentence().contains(w.getWord())) {
                    // если входит, то занести это предложение 
                    // в список предложений для слова                    
                    w.addSentence(s);
                }
            }
        }
        log("Составление списка предложений в которых есть слово завершено!");
        
        allSentences.clear();
        allSentences = null;
        
        StringBuffer outp = new StringBuffer();
        String firstLetter = "";
        
        // файл для вывода сохраненяемого конкорданса            
        File outFile = null;
        RTFFileWriter writer = null;
        
        File notFound = new File("./notfound.txt");
        PrintStream notFoundOut = null;
        try {
            notFoundOut = new PrintStream(new FileOutputStream(notFound));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        long totalCounter = 0;
        for (ConcWord w : allWords) {
            // получить букву
            String first = w.getWord().substring(0, 1).toLowerCase();
            first = StringFilter.escapeFileBadChars(first);
            if (firstLetter.length()==0 || !first.equals(firstLetter)) {
                
                if (!first.equals(firstLetter) & writer!=null) {                
                    writer.writeRtfFooter();
                }

                firstLetter = first; 
                try {                    
                    outFile = new File(docPath + "/" + resultName + "_" + firstLetter + ".rtf");   
                    writer = new RTFFileWriter(outFile);
                    outp.delete(0, outp.length());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    //todo: create normal way to avoid such construction
                    continue;
                }
            }

            //todo: create normal way to avoid such construction
            if (outFile==null || writer==null) {
                continue;
            }

            // workaround. TODO Найти лучшее решение для избавления от слов, 
            // количество которых = 0
            if (w.getManualNumberOfOccurences() == 0) {
                notFoundOut.println(w.getWord());
            }
            outp.append("\\b ");
            outp.append(w.getWord().trim());
            outp.append(" : ");
            outp.append(w.getManualNumberOfOccurences());
            outp.append("\\b0 ");
            outp.append("\r\n");
            
            // TODO: replace with action listener
            log(w.getWord() + " : " + w.getManualNumberOfOccurences());
            String prevSentence = null;
            for (ConcSentence s : w.getSentences()) {
                
                //выделить все найденные слова в предложении подчеркиванием
                String underScoreWords = s.getSourceSentence();                 

                // проверить на разрывы и дополнить если необходимо
                if (s.isBroken()) {
                    if (s.getPreviousSentence() != null) {
                        underScoreWords = s.getPreviousSentence().getSourceSentence() + underScoreWords; 
                    }
                    if (s.getNextSentence() != null) {                        
                        underScoreWords = underScoreWords + s.getNextSentence().getSourceSentence() ;
                    }
                }

                // пропустить дубликат предложения
                if (prevSentence != null && underScoreWords.equals(prevSentence)) {
                    continue;                    
                }
                
                prevSentence = underScoreWords;
                
                underScoreWords = underScoreWords.replaceAll("\n", " ");
                underScoreWords = underScoreWords.trim();                
                underScoreWords = StringFilter.underLineWords(underScoreWords, w.getSourceWord().trim());               
                
                outp.append(underScoreWords)
                        .append("\\b ")
                        .append("(")
                        .append(s.getReference().getVolumeNumber()) 
                        .append(", ")
                        .append(s.getReference().getPageNumber())
                        .append(")")
                        .append("\\b0 ")
                        .append("\r\n");
            }
            outp.append("\r\n\r\n");
            
            writer.writeBodyText(outp.toString());
            totalCounter = totalCounter + outp.length();
            outp.delete(0, outp.length());
        }
        
        log("Total characters: " + totalCounter);
        
        if (notFoundOut!=null) {
            notFoundOut.flush();
            notFoundOut.close();
        }
        
        if (writer!=null) {
            writer.writeRtfFooter();
        }
        
        if (outFile!=null) {
            result = outFile.getAbsolutePath();            
        }
        
        return result;
    }  

    public String getDocPath() {
        return docPath;
    }

    public void setDocPath(String docPath) {
        this.docPath = docPath;
    }

    public void setResultName(String name) {
        this.resultName = name;        
    }

}
