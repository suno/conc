package ru.suno.concordance.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import ru.suno.concordance.utils.PagesProcessor;

public class TestSeparatedSentences extends TestCase {

	private static final String TEST_RESULT_FILENAME = "result";
	public static final String CONCORDANCE_TESTDATA = "/home/suno/eclipse_workspace/Concordance/testdata/";

	/**
	 * @param args
	 */
	public void testSimpleFileParse() {
		Utils.clearDir(new File(CONCORDANCE_TESTDATA), TEST_RESULT_FILENAME);
		
		PagesProcessor pp = new PagesProcessor(null);
		assert(pp != null);
		
		pp.setDocPath(CONCORDANCE_TESTDATA);
		pp.setResultName(TEST_RESULT_FILENAME);
		
		List<File> fileList = new ArrayList<File>();
		fileList.add(new File("/home/suno/eclipse_workspace/Concordance/testdata/test1.doc"));
		
		pp.processAllDocuments(fileList);
		//fileList.add(new File("/home/suno/eclipse_workspace/Concordance/testdata/test1.doc"));
		//fileList.add(new File("/home/suno/eclipse_workspace/Concordance/testdata/test1.doc"));
		
	}

}
