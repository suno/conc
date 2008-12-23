package ru.suno.concordance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import ru.suno.concordance.parser.StringFilter;

/**
 * Предложение для конкорданса. 
 * */
public class ConcSentence {    
    private ConcRef reference;
    
    private int sentBeginPos;    
    private int sentEndPos;    
    private boolean broken = false;
    
    private ConcSentence previousSentence = null;   
    private ConcSentence nextSentence = null;
    
    public ConcSentence (String sentence) {
        setSourceSentence(sentence);
    }
        
    /** Предложение в виде коллекции строк. */
    private Collection<String> m_sentence = new ArrayList<String>();
    
    private String m_sourceSentence;
    
    /** Возвращает предожение для конкорданса в виде массива строк. */
    public Collection<String> getSentence() {
        return m_sentence;
    }

    public String getSourceSentence() {
        return m_sourceSentence;
    }

    private void setSourceSentence(String sourceSentence) {
        m_sourceSentence = sourceSentence;

        m_sentence.clear();
        String workSentence = StringFilter.replacePunctChars(sourceSentence);
        
        StringTokenizer st = new StringTokenizer(workSentence);
        String word;
        while (st.hasMoreTokens()) {
            word = st.nextToken();
            // TODO: доделать capitalization
            word = word.toLowerCase();
            
            m_sentence.add(word);
        }
    }

    /** Получить ссылку. */
    public ConcRef getReference() {
        return reference;
    }

    /** Установить ссылку. */
    public void setReference(ConcRef reference) {
        this.reference = reference;
    }

    public int getSentBeginPos() {
        return sentBeginPos;
    }

    public void setSentBeginPos(int sentBeginPos) {
        this.sentBeginPos = sentBeginPos;
    }

    public int getSentEndPos() {
        return sentEndPos;
    }

    public void setSentEndPos(int sentEndPos) {
        this.sentEndPos = sentEndPos;
    }

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean isBroken) {
        this.broken = isBroken;
    }

    public ConcSentence getPreviousSentence() {
        return previousSentence;
    }

    public void setPreviousSentence(ConcSentence previousSentence) {
        this.previousSentence = previousSentence;
    }

    public ConcSentence getNextSentence() {
        return nextSentence;
    }

    public void setNextSentence(ConcSentence nextSentence) {
        this.nextSentence = nextSentence;
    }
    

}
