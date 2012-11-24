/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2012. Nigel Cook. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * Licensed under the terms described in LICENSE file that accompanied this code, (the "License"); you may not use this file
 * except in compliance with the License. 
 * 
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on 
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 *  specific language governing permissions and limitations under the License.
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
