package ru.suno.concordance.utils;

import java.io.File;

public interface IWordFile {

	/**
	 * @param fileName Word file to read, with complite path, example: <code>c://dir//file.doc</code>.
	 * */
	public abstract String readWordFileContent(File inFile);

	public abstract void writeTextFileContent(File outFile, String content);

	public abstract void writeWordFileContent(String content, String outFile);

}