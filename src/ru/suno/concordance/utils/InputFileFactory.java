package ru.suno.concordance.utils;

public class InputFileFactory {
	
	public enum FILE_FORMAT {
		MS_WORD_97,
		MS_WORD_2007
	};
	
	public static FILE_FORMAT _fileFormat = FILE_FORMAT.MS_WORD_97;
	
	public static IWordFile getWordFile() {
		IWordFile file = null;
		
		switch(_fileFormat) {
		case MS_WORD_2007:
			file = new MSWordVistaFile();
			break;
		case MS_WORD_97:
			file = new MSWordFile();
			break;
		}
		
		return file;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	}
}
