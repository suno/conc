package ru.suno.concordance;


public class ConcPage {
    
    /** Ссылка (номер тома, номер страницы). */
    private ConcRef reference;
    
    /** Текст на странице. */
    private String text;

    public ConcRef getReference() {
        return reference;
    }

    public void setReference(ConcRef ref) {
        this.reference = ref;
    }

    public String getText() {
        return text;
    }

    public void setText(String i_text) {
        text = i_text;
    }
}
