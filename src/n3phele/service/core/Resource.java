/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.core;

import java.util.Locale;
import java.util.ResourceBundle;

public class Resource {
	private static Resource resource = null;
	protected ResourceBundle bundle;
	private Resource() {
		try {
			bundle =
				ResourceBundle.getBundle("n3phele.resource.service",
				Locale.getDefault(), this.getClass().getClassLoader());	
		} catch (Exception e) {
		}
	}
	public static String get(String key, String defaultValue) {
		String result = defaultValue;
		if(resource == null) {
			resource = new Resource();
		}
		try {
			result = resource.bundle.getString(key);
		} catch (Exception e) {
			result = defaultValue;
		}
		return result;
	}
	
	public static boolean get(String key, boolean defaultValue) {
		boolean result = defaultValue;
		if(resource == null) {
			resource = new Resource();
		}
		try {
			String field = resource.bundle.getString(key);
			result = Boolean.valueOf(field);
		} catch (Exception e) {
			result = defaultValue;
		}
		return result;
	}

}
