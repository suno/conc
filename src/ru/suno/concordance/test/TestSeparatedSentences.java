package ru.suno.concordance.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.suno.concordance.utils.PagesProcessor;

public class TestSeparatedSentences {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PagesProcessor pp = new PagesProcessor(null);
		
		pp.setDocPath("/home/suno/eclipse_workspace/Concordance/testdata/");
		pp.setResultName("result");
		
		List<File> fileList = new ArrayList<File>();
		fileList.add(new File("/home/suno/eclipse_workspace/Concordance/testdata/test1.doc"));
		
		pp.processAllDocuments(fileList);
		//fileList.add(new File("/home/suno/eclipse_workspace/Concordance/testdata/test1.doc"));
		//fileList.add(new File("/home/suno/eclipse_workspace/Concordance/testdata/test1.doc"));
		
	}

}
