package ru.suno.concordance.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.suno.concordance.ConcWord;
import ru.suno.concordance.utils.IWordFile;
import ru.suno.concordance.utils.MSWordFile;

/**
 * Разбивает текст на слова и предложения, связывает каждое слово с одним
 * или несколькими предложениями.
 * */
public class WordParser {
    
    //private String m_addSymbols;
    private Collection<Character> str;
    
    private String readAdditionalSymbols() {
        String m_addSymbols;
        
        if (str == null) {
            str = new ArrayList<Character>();
            //StringBuffer result = new StringBuffer();
            File addSymbolsFile = new File("./add/symbols.doc");
            IWordFile wordReader = new MSWordFile();
            m_addSymbols = wordReader.readWordFileContent(addSymbolsFile);
            
            m_addSymbols = m_addSymbols.replaceAll("\r\n", "");
            
            for (int i=0; i < m_addSymbols.length(); i++) {                
                if (!str.contains(m_addSymbols.charAt(i))) {
                    str.add('\\');
                    str.add(m_addSymbols.charAt(i));
                }
            }
        }
        
        StringBuffer result = new StringBuffer();
        
        for (Iterator<Character> iterator = str.iterator(); iterator.hasNext();) {
            Character character = iterator.next();
            result.append(character);
        }
        
        return result.toString();
    }

    /**
     * Returns collection of (concordance)words from given string.
     * @param file Pre-formatted text to parse.
     * @return Collection of (concordance)words from text.
     */
    public final Collection<ConcWord> getAllWords(String text) {        
        final List<ConcWord> result = new ArrayList<ConcWord>();

        // Только слова, без чисел
        Pattern pattWord = 
            Pattern.compile("[A-Za-zА-Яа-я" + readAdditionalSymbols() +"]+");
        Matcher matchWord = pattWord.matcher(text);
        String word;
        Collection<ConcWord> allWords = new ArrayList<ConcWord>();
        while (matchWord.find()) {
            word = text.substring(matchWord.start(), matchWord.end());
            
            // TODO: найти лучший способ для фильтрации спец. символов
            //word = StringFilter.replaceAtBounds(word, ")");
            //word = StringFilter.replaceAtBounds(word, "(");
            //word = BoundsFilter.replaceAtBounds(word, "[");
            //word = BoundsFilter.replaceAtBounds(word, "]");
            
            ConcWord concWord = new ConcWord(word);
            allWords.add(concWord);
        }
        
        // отсортировать слова по алфавиту
        // расположить их в алфавитном порядке
        result.addAll(allWords);
        Collections.sort(result);        
        allWords.clear();
        
        return result;
    }
    
    public static void main(String argc[]) {
        /*WordParser wp = new WordParser();
        wp.readAdditionalSymbols();*/
        String wp = "\u00Df";
        System.out.println(wp);
    }
}
