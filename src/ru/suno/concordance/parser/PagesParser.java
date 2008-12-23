package ru.suno.concordance.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.suno.concordance.ConcPage;
import ru.suno.concordance.ConcRef;

public class PagesParser {
    
    public Collection<ConcPage> parseTextToPages(String allText) {
        Collection<ConcPage> result = new ArrayList<ConcPage>();
                    
        Pattern pattReference = Pattern.compile("\\d+,\\s+\\d+\\r\\n");
        Matcher matchReference = pattReference.matcher(allText);
        
        // найти первую ссылку
        if (matchReference.find()) {
            // нашли первую ссылку, запомнить ее конец
            int firstRefEnd = matchReference.end();
            // создать ссылку 1
            String reference = allText.substring(matchReference.start(), firstRefEnd);  
            String volStr = reference.substring(0, reference.indexOf(","));
            String pageStr = reference.substring(reference.indexOf(",") + 1, reference.length() - (reference.indexOf(",") + 1));
            ConcRef ref = new ConcRef(Integer.parseInt(volStr.trim()), Integer.parseInt(pageStr.trim()));
            
            ConcPage page = null;
            while (matchReference.find()) {    
                // создать и занести в массив страницу от конца предыдущей ссылки до начала следующей
                page = new ConcPage();
                // ccылка - предыдующая найденная
                page.setReference(ref);
                page.setText(allText.substring(firstRefEnd, matchReference.start()));
                // занести страницу в массив
                result.add(page);
                // создать очередную найденную ссылку
                firstRefEnd = matchReference.end();
                reference = allText.substring(matchReference.start(), firstRefEnd);  
                volStr = reference.substring(0, reference.indexOf(","));
                pageStr = reference.substring(reference.indexOf(",") + 1, reference.length() - (reference.indexOf(",")));
                ref = new ConcRef(Integer.parseInt(volStr.trim()), Integer.parseInt(pageStr.trim()));
            } 
            // занести последнюю страницу
            page = new ConcPage();
            // ccылка - предыдующая найденная
            page.setReference(ref);
            page.setText(allText.substring(firstRefEnd, allText.length()));
            // занести страницу в массив
            result.add(page);                
        }             
        return result;
    }
}
