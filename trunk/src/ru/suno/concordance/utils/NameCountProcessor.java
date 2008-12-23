package ru.suno.concordance.utils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ru.suno.concordance.ConcWord;
import ru.suno.concordance.ConcWordList;
import ru.suno.concordance.dlg.FileSelectDialog;
import ru.suno.concordance.parser.WordParser;

public class NameCountProcessor {
	private String docPath = "";
	private String resultName;

	// TODO: replace FileSelectDialog argument with action listener
	public String processAllDocuments(List<File> srcList, FileSelectDialog fileSelectDialog) {
		String result = "";

		// прочесть текст в буфер из всех файлов
		IWordFile wr = InputFileFactory.getWordFile();
		String allText = "";
		for (File file : srcList) {
			String fileText = wr.readWordFileContent(file);
			if (fileText != null) {
				allText += fileText;
			}
		}
		
		// разбить ВЕСЬ текст на слова
		List<ConcWord> allWords = new ConcWordList();
		WordParser wp = new WordParser();
		Collection<ConcWord> wrds = wp.getAllWords(allText);
		
		ConcWord countWord = null;
		if (wrds != null) {
			// добавить найденные в тексте слова
			for (ConcWord w : wrds) {
				if (!allWords.contains(w)) {
					// слова нет в списке
					countWord = w;
					countWord.incrementNumberOfOccurences();					
					allWords.add(countWord);					
				} else {
					// слово есть в списке
					countWord.incrementNumberOfOccurences();					
				}
			}
		}
		
		// отсортировать словоформы по количеству употреблений
		Collections.sort(allWords, new Comparator<ConcWord>() {
			public int compare(ConcWord w1, ConcWord w2) {
				int result = 0;
				if (w1.getManualNumberOfOccurences() < w2.getManualNumberOfOccurences()) {
					result = 1;
				} else {
					result = -1;
				}
				return result;
			}
		});


		// файл для вывода сохраненяемого конкорданса
		File outFile = null;
		RTFFileWriter writer = null;
		
		outFile = new File(docPath + "/" + resultName + ".rtf");

		try {
			writer = new RTFFileWriter(outFile);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		StringBuffer outp = new StringBuffer();
		for (ConcWord w : allWords) {
            outp.append(w.getWord().trim());
            outp.append(" : ");
            outp.append(w.getManualNumberOfOccurences());
            outp.append("\r\n");
		}
		
		if (writer != null) {
			writer.writeBodyText(outp.toString());
			writer.writeRtfFooter();
		}

		if (outFile != null) {
			result = outFile.getAbsolutePath();
		}

		return result;
	}

	public String getDocPath() {
		return docPath;
	}

	public void setDocPath(String docPath) {
		this.docPath = docPath;
	}

	public void setResultName(String name) {
		this.resultName = name;
	}

}
