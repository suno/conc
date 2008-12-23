package ru.suno.concordance.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.Range;

import ru.suno.concordance.parser.StringFilter;

public class MSWordFile implements IWordFile {
    
    /* (non-Javadoc)
	 * @see ru.suno.concordance.utils.IWordFile#readWordFileContent(java.io.File)
	 */
    public String readWordFileContent(File inFile) {
        String result = null;
        InputStream in = null;
        try {
            in = new FileInputStream(inFile);
            in = new BufferedInputStream(in);
            HWPFDocument wordDoc = new HWPFDocument(in);
            WordExtractor extractor = new WordExtractor(wordDoc);
            result = extractor.getText();
            //result = extractor.getTextFromPieces();
            result = result.replaceAll("\f", "\n\r");
            
            result = StringFilter.convertGreekCharsToUTF(result);
            
            in.close();
        } catch (IOException ex) {
            System.out.println("Error while reading source word file!");
            ex.printStackTrace(System.out);
        }
        // пройдемся по тексту, попробуем найти выделенные жирным шрифтом номера тома/страницы 
        return result;
    }
    
    /* (non-Javadoc)
	 * @see ru.suno.concordance.utils.IWordFile#writeTextFileContent(java.io.File, java.lang.String)
	 */
    public void writeTextFileContent(File outFile, String content) {
        FileOutputStream fos;
        OutputStreamWriter out;
        try{
            fos = new FileOutputStream(outFile);
            out = new OutputStreamWriter(fos);
            out.write(content);            
            out.close();
            fos.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /* (non-Javadoc)
	 * @see ru.suno.concordance.utils.IWordFile#writeWordFileContent(java.lang.String, java.lang.String)
	 */
    public void writeWordFileContent(String content, String outFile) {
        try {
         // Create the encoder and decoder for ISO-8859-1
            CharsetEncoder enc = Charset.forName("utf-8").newEncoder();
            enc.onMalformedInput(CodingErrorAction.REPORT);
            enc.onUnmappableCharacter(CodingErrorAction.REPLACE);
            byte[] gbBytes = enc.encode(CharBuffer.wrap(content)).array();
            
            /*String encoding = "Cp1252";
            if (piece.usesUnicode()) {
                encoding = "UTF-16LE";
            }*/
            
            String text = new String(gbBytes, "utf-8");
            /*Writer outwr = new OutputStreamWriter(new 
                    FileOutputStream(outFile),"utf-8");
                    file.writeAllText(out);*/
                    
            HWPFDocument templateFile = new HWPFDocument(
                    new FileInputStream("./add/blank.doc"));        
            OutputStream outGenerated = new FileOutputStream(outFile);    
            templateFile.write(outGenerated);
            
            OutputStream out = new FileOutputStream(new File(outFile));           
            Range blankRange = templateFile.getRange();
            //Range templateRange = templateFile.getRange();
            //String templateContent = templateRange.text();
            blankRange.insertBefore(text);
            templateFile.write(out);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
