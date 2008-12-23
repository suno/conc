package ru.suno.concordance.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class RTFFileWriter {
    
    private PrintWriter outWriter = null;
    private File outFile = null;

    public RTFFileWriter(File i_outFile) {
        outFile = i_outFile;
        try {
            outWriter = new PrintWriter(outFile);
            writeRtfHeader("Test document title", "Author Name");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Converts string to RTF escape sequence.
     * <p>Supports unicode characters.
     * But doesn't handle correctly control symbols and CR LF.
     */
    public String escape(String javaString)
    {
        StringBuffer ret = new StringBuffer();
        for (int i=0; i < javaString.length(); i++) 
        {
            char c = javaString.charAt(i);
            if (c < 128) {
                ret.append(c);
            } else {
                int ic = (int) c;
                // write non-ascii character 
                ret.append("\\u").append(ic).append(' '); 
            }
        }
        if (ret.length() > 0) {
            ret.insert(0, "\\uc0 "); // change unicode count to 0
        }
        return ret.toString();
    }
 
    public void writeRtfHeader(String title, String author) {
        outWriter.println("{\\rtf1");
        outWriter.print("{\\info{\\title "+escape(title)+"}");
        outWriter.println("{\\author "+escape(author)+"}}");
    }
 
    public void writePageHeaderFooter(PrintWriter out, String pageHeader)
    {
        out.println("{\\header\\pard\\qc{\\fs50 "+escape(pageHeader)+
                    "\\par}{\\fs18\\chdate\\par}\\par\\par}");
        out.println("{\\footer\\pard\\qc\\brdrt\\brdrs\\brdrw10\\brsp100"+
                    "\\fs18 Page " +
                    "{\\field{\\*\\fldinst PAGE}{\\fldrslt 1}} of " +
                    "{\\field{\\*\\fldinst NUMPAGES}{\\fldrslt 1}} \\par}");
    }
 
    public void writeRtfFooter() {
        outWriter.println("}");
        outWriter.flush();
        outWriter.close();
        
        outWriter = null;
        outFile = null;        
    }
    
    public void writeBodyText(String text) {
        String text2 = escape(text);
        text2 = text2.replaceAll("\r\n", "\\\\par ");           
        outWriter.println(text2);        
    }
}
