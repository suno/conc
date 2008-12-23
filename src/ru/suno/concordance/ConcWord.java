package ru.suno.concordance;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Слово для конкорданса. Содержит в себе список предложений в котором встречается.
 * */
public class ConcWord implements Comparable<ConcWord> {
    
    /** Само слово. */
    private String m_word;
    
    private String m_sourceWord;
    
    private int m_numberOfOccurences = 0;

    /** Список предложений в котором слово встречается. */
    private Collection<ConcSentence> m_sentences = new ArrayList<ConcSentence>();
    
    /** Конструктор можно вызвать только передав слово в качестве аргумента. */
    public ConcWord(String i_word) {
        // TODO: доделать capitalization
        m_sourceWord = i_word;
        m_word = i_word.toLowerCase();
    }
    
    /**
     * @return Список предложений в которых это слово встречается.
     * */
    public Collection<ConcSentence> getSentences() {
        return m_sentences;
    }

    /**
     * Добавить предложение в список где слово можно встретить. 
     * */
    public void addSentence(ConcSentence sen) {
        this.m_sentences.add(sen);
    }
    
    /**
     * Получить слово.
     * */
    public String getWord() {
        return m_word;
    }   
    
    public int getManualNumberOfOccurences() {
		return m_numberOfOccurences;
	}

	public void setManualNumberOfOccurences(int ofOccurences) {
		m_numberOfOccurences = ofOccurences;
	}

	public int incrementNumberOfOccurences() {
		m_numberOfOccurences++;
		return m_numberOfOccurences;
	}
	
	/**
     * Для сортировки.
     * */
    @Override
    public int compareTo(ConcWord w) {
        int result = this.getWord().toLowerCase().compareTo(w.getWord().toLowerCase());
        return result;
    }

    public String getSourceWord() {
        return m_sourceWord;
    }
}
