package org.malibu.util;

import java.io.File;

public class Util {
	public static String getJarDirectory() {
		// get directory .jar file is running from (using substring() to remove leading slash)
		String workingDir = Util.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		File file = new File(workingDir);
		workingDir = file.getAbsolutePath();
		if(workingDir.startsWith("\\")) {
			workingDir = workingDir.substring(1);
		}
		if(workingDir.endsWith(".")) {
			workingDir = workingDir.substring(0, workingDir.length() - 2);
		}
		return workingDir;
	}
}
