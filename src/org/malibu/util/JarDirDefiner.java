package org.malibu.util;

import ch.qos.logback.core.PropertyDefinerBase;

public class JarDirDefiner extends PropertyDefinerBase {
	public String getPropertyValue() {
		return Util.getJarDirectory();
	}
}
