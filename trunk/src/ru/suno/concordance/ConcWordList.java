package ru.suno.concordance;

import java.util.ArrayList;

public class ConcWordList extends ArrayList<ConcWord> {

    /** For class serialization. */
    private static final long serialVersionUID = -3137993695342745413L;

    @Override
    public boolean contains(Object arg0) {
        boolean result = false;
        
        String compareTo = null;
        if (arg0 instanceof String) {            
            compareTo = (String) arg0;
        } else if (arg0 instanceof ConcWord) {
            compareTo = ((ConcWord) arg0).getWord();        
        }

        if (compareTo != null) {
            for (int i = 0; i < this.size(); i++) {
                if (this.get(i).getWord().equals(compareTo)) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

}
