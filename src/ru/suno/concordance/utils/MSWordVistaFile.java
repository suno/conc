package ru.suno.concordance.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;

public class MSWordVistaFile implements IWordFile {
    
    /* (non-Javadoc)
	 * @see ru.suno.concordance.utils.IWordFile#readWordFileContent(java.io.File)
	 */
    public String readWordFileContent(File inFile) {
        String result = null;
        return result;
    }
    
    /* (non-Javadoc)
	 * @see ru.suno.concordance.utils.IWordFile#writeTextFileContent(java.io.File, java.lang.String)
	 */
    public void writeTextFileContent(File outFile, String content) {
        FileOutputStream fos = null;
        OutputStreamWriter out = null;
        try{
            fos = new FileOutputStream(outFile);
            out = new OutputStreamWriter(fos);
            out.write(content);
            out.flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
				out.close();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}                    	
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
