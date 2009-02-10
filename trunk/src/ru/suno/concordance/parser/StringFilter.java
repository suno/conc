package ru.suno.concordance.parser;


public class StringFilter {
    public static String preprocessSentence(String src) {
        String result = "";
        result = src.replaceAll("\\s+\\.\\s?+", ". ");        
        //result = result.replaceAll("\\f", "TheBigPageBreak");
        
        return result;
    }
    
    
    public static String replaceAtBounds(String src, String charToReplace) {
        String result = src;
        
        if (result.indexOf(charToReplace) == 0 && 
            result.length() > 1) {
            result = result.substring(1, result.length());
        }
        
        if (result.indexOf(charToReplace) == result.length()-1 &&
            result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }        
        
        return result;
    }
    
    public static String replacePunctChars(String src) {
        String result = src;
        
        result = result.replaceAll("\\?", " \\? ");
        result = result.replaceAll("\\!", " \\! ");
        result = result.replaceAll("\\.", " \\. ");
        result = result.replaceAll("\\,", " \\, ");
        result = result.replaceAll("\\;", " \\; ");
        result = result.replaceAll("\\:", " \\: ");

        return result;
    }
    
    /** Эскейпит специальные символы используемые в регулярных выражениях: ^$.|?*+(). */
    public static String escapeRegexpChars(String i_src) {
        String result = i_src;
        result = result.replaceAll("\\\\", "\\\\\\\\");
        result = result.replaceAll("\\[", "\\\\[");
        result = result.replaceAll("\\]", "\\\\]");
        result = result.replaceAll("\\^", "\\\\^");
        result = result.replaceAll("\\$", "\\\\$");
        result = result.replaceAll("\\.", "\\\\.");
        result = result.replaceAll("\\|", "\\\\|");
        result = result.replaceAll("\\?", "\\\\?");
        result = result.replaceAll("\\(", "\\\\(");
        result = result.replaceAll("\\)", "\\\\)");
        result = result.replaceAll("\\*", "\\\\*");
        result = result.replaceAll("\\+", "\\\\+");
        return result;        
    }
    
    private static boolean isHaveBound(String sentence, int bound) {
        boolean result = false;
        
        if (bound < 0) {
            return true;
        }
        
        if (bound >= sentence.length()) {
            return true;
        }
        
        char testRight = sentence.charAt(bound);
        if (testRight == ' ' || testRight == '.' || 
            testRight == ',' || testRight == ';' ||
            testRight == '!' || testRight == '?') {
            result = true;
        }
        
        return result;
    }
    
    /** Возвращает предложение с подчеркнутыми словами. */
    public static String underLineWords(String sentence, String word) {
        String result = sentence.trim();
        //String lowercaseSentence = sentence.trim();
        
        int cursor = 0;
        while (cursor < result.length() ) {
            // нашли начальный индекс...
            cursor = result.toLowerCase().indexOf(word.toLowerCase(), cursor);
            if (cursor == -1) {
                break;
            }
            
            //проверить есть ли у этого слова правая и левая граница
            int boundSymbolIndexRight = cursor + word.length();
            int boundSymbolIndexLeft = 0;
            boundSymbolIndexLeft = cursor - 1;
            
            if (isHaveBound(result, boundSymbolIndexLeft) & isHaveBound(result, boundSymbolIndexRight)) {
                String ul = "\\ul " + result.substring(cursor, boundSymbolIndexRight) + "\\ulnone ";
                result = result.substring(0, cursor) + ul + result.substring(cursor + word.length());
                cursor++;
            } else {
                cursor++;
            }
        }
        
        return result;
    }
    
    public static String escapeFileBadChars(String src) {
        String result = src;
        
        result = result.replaceAll("\\\\", "slash");        
        result = result.replaceAll("\\/", "backslash");
        result = result.replaceAll("<", "lesser");
        result = result.replaceAll(">", "greater");
        result = result.replaceAll("\\*", "asterik");
        result = result.replaceAll("\\\"", "doublequote");
        result = result.replaceAll(":", "colon");
        result = result.replaceAll("\\|", "verticalline");
        result = result.replaceAll("\u03B5", "greek_E");
        
        return result;
    }
    
    public static String convertGreekCharsToUTF(String src) {
        String result = src;
        
        for (int i=0;i<result.length();i++) {
            switch (result.codePointAt(i)) {
            case 0xF062: //beta
                result = result.replace(result.charAt(i), '\u03b2');
                break;            
            }
            /*if (result.codePointAt(i)==61538) { //greek beta
                result = result.replace(result.charAt(i), '\u03b2');
            }*/
        }
        
        return result;
    }
    
    public static void main(String []args) {
       /* System.out.println(
                StringFilter.escapeFileBadChars("\\_some_/_some_<_some_>_some_*_some_\"_some_:_some")
                );*/
    }
}
