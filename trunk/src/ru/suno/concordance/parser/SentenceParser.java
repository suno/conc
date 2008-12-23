package ru.suno.concordance.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.suno.concordance.ConcRef;
import ru.suno.concordance.ConcSentence;
import ru.suno.concordance.utils.IWordFile;
import ru.suno.concordance.utils.MSWordFile;

public class SentenceParser {
    
    Collection<String> exclusions = null;
    
    private Collection<String> getAllExclusions() {
        if (exclusions==null) {
            exclusions = new ArrayList<String>();
            File addSymbolsFile = new File("./add/exeptions.doc");
            IWordFile wordReader = new MSWordFile();            
            String allExcepts = wordReader.readWordFileContent(addSymbolsFile);
                        
            StringTokenizer st = new StringTokenizer(allExcepts, "\r\n");       
            while (st.hasMoreTokens()) {
                exclusions.add(st.nextToken().trim());
            }
        }
        
        return exclusions;
    }
    
    /**
     * Получает коллекцию предложений из текста. Предложения разделены точками.
     * Сокращения концом предложения не считаются.
     * */
    public final Collection<ConcSentence> getAllSentencies(final String srcText, final ConcRef ref, final ConcSentence prevSentence) {        
        Collection<ConcSentence> result = new ArrayList<ConcSentence>();
        
        ConcSentence previousSentence = prevSentence;
        
        String text = StringFilter.preprocessSentence(srcText);
        
        Pattern pattSentenceEnd = Pattern.compile("(\\.|\\?|\\!|\\n\\r)+");
        Matcher matchSentenceEnd = pattSentenceEnd.matcher(text);
        
        int sentenceStart = 0;
        int sentenceEnd = 0;
        String sentence;
        String lastWord;
        
        boolean isPreviousBroken = false;
        if (prevSentence!=null) {
            isPreviousBroken = prevSentence.isBroken();
        }

        ConcSentence sent = null;
        while (matchSentenceEnd.find()) {
            // Получить предложение
            sentenceEnd = matchSentenceEnd.end();
            sentence = text.substring(sentenceStart, sentenceEnd);
            
            // получить последнее слово в предложении
            lastWord = findLastWord(sentence);

            // проверить, не является ли последнее найденное слово исключением
            if (getAllExclusions().contains(lastWord)) {
                // слово является исключением. перейти к следующему слову.
                continue;
            }
               
            // занести предложение в массив
            sent = new ConcSentence(sentence);
            
            // если в предыдущем предложении разрыв - то в следующем тоже.
            if (isPreviousBroken) {
                sent.setBroken(true);
                isPreviousBroken = false;
            }
            
            sent.setPreviousSentence(previousSentence);
            
            sent.setSentBeginPos(matchSentenceEnd.start());
            sent.setSentEndPos(matchSentenceEnd.end());
            sent.setReference(ref);
            result.add(sent);
            
            if (previousSentence!=null) {
                previousSentence.setNextSentence(sent);
            }
            previousSentence = sent;
                
            // Получить позицию где начинается следующее предложение 
            sentenceStart = matchSentenceEnd.end();
            if (sentenceStart + 1 <= text.length()) {
                sentenceStart++;
            }
        }
        
        // проверить, осталось ли еще что-нибудь в конце...
        if (sentenceStart < text.length()) {            
            // определить конец предложения

            // если нет, то использовать остаток текста как предложение до конца
            sentence = text.substring(sentenceStart, text.length());
            
            // занести предложение в массив            
            sent = new ConcSentence(sentence);
            sent.setReference(ref);
            result.add(sent);                
        }
        
        // проверить на разрыв страницы...
        if (sent!=null) {
            for (int cursor = sent.getSourceSentence().length() - 1; cursor > 0; cursor--) {
                char c = sent.getSourceSentence().charAt(cursor);
                 
                if (!Character.isWhitespace(c) & !Character.isLetterOrDigit(c)) {                
                    if (c == '.') {
                        // проверить, не сокращение ли это...
                        int startShortPos = sent.getSourceSentence().lastIndexOf(' ', cursor);
                        if (startShortPos == -1) {
                            startShortPos = 0;
                        }                    
                        String possibleShorting = sent.getSourceSentence().substring(startShortPos, cursor);
                        // нашли сокращение на месте разрыва предложения
                        if (getAllExclusions().contains(possibleShorting)) {
                            sent.setBroken(true);
                            break;
                        } else {
                            sent.setBroken(false);
                            break;
                        }
                    } else if (c != '?' && c != '!') {
                        sent.setBroken(true);
                        break;                    
                    } else {
                        sent.setBroken(false);
                        break;
                    }
                }   
                
                if (Character.isLetterOrDigit(c)) {
                    sent.setBroken(true);
                    break;                                    
                }
            }
        }

        // пройтись по всем словам где есть точка. 
        return result;
    }
    
    
    public String findLastWord(String text) {
        String result = "";
        
        // найти начало слова от точки
        StringTokenizer st = new StringTokenizer(text); //,"\r\n?!. ");       
        while (st.hasMoreTokens()) {
            result = st.nextToken();
        }
        
        // quick fix for end of sentence... replace later
        if (result.trim().endsWith("!") || result.trim().endsWith("?")) {
            StringTokenizer fixString = new StringTokenizer(result, "?!");
            while (fixString.hasMoreTokens()) {
                result = fixString.nextToken();
            }
        }
        
        return result;
    }
}
