package ru.suno.concordance.test;

import java.io.File;

public class Utils {
	public static boolean clearDir(File path, String aFileNamePattern) {        
		if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (File file : files) {
                    // to avoid the mindf$#k
                    if (file.isDirectory() && !file.getName().startsWith("..") && !file.getName().startsWith("/")) {
                        clearDir(file, aFileNamePattern);
                    } else {
                    	if (file.getName().contains(aFileNamePattern)) {
                    		file.delete();
                    	}
                    }
                }
            }
        }
        return (path.delete());
    }
}
