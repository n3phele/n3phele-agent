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
package n3phele.agent.repohandlers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import n3phele.agent.model.Origin;

import com.amazonaws.services.s3.internal.Mimetypes;

public class LocalFile implements Repo {
	private static Logger log = Logger.getLogger(LocalFile.class.getName());
	private Long length = null;
	private String encoding;
	private String root;
	private String key;
	private File file;
	private Long modificationTime = null;
	private File baseFile = null;
	private Long totalLength = null;
	private URI source;
	private String tag;
	private String description;
	String kind;

	public LocalFile(String tag, String description, String accessKey, String secretKey, URI source, String kind, String root, String key) {
		this.tag = tag;
		this.description = description;
		this.source = source;
		this.kind = kind;
	
		if(root == null || root == "") {
			root = ".";
		}
		this.root = root;
		if(key == null)
			key = "";
		this.key = key;
		this.file = new File(this.root+File.separator+this.key);
		this.encoding = Mimetypes.getInstance().getMimetype(this.file);
	}
	

	/* (non-Javadoc)
	 * @see n3phele.agent.repohandlers.Repo#getInputStream(java.lang.String, java.lang.String)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		this.encoding = null;
		log.info("Input: "+file.getCanonicalPath());
       return new FileInputStream(file);
	}
	

	/* (non-Javadoc)
	 * @see n3phele.agent.repohandlers.Repo#put(java.lang.String, java.lang.String, java.io.InputStream, long, java.lang.String)
	 */
	@Override
	public Origin put(InputStream input, long length,
			String encoding) throws IOException {
		Origin result = new Origin(this.root+File.separator+this.key, 0, null, null);
		File path = file.getCanonicalFile().getParentFile();
		if(!path.exists()) {
			path.mkdirs();
		}
		DataInputStream reader = new DataInputStream(input);
		DataOutputStream writer = new DataOutputStream(new FileOutputStream(file));
		byte[] buffer = new byte[8 * 4096];
		try {
			int len;
			while ((len = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, len);
			}
			result.setLength(file.length());
			result.setModified(new Date(file.lastModified()));
		} catch (IOException e) {
			log.log(Level.SEVERE, "File " + file.getCanonicalPath(), e);
			throw e;
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
			} finally {	
				try {
					reader.close();
				} catch (IOException e) {				
				} finally {
					if(this.modificationTime != null) {
						file.setLastModified(this.modificationTime);
						result.setModified(new Date(this.modificationTime()));
					}
				}
			}
		}
		return result;
	}


	@Override
	public long getLength() {
		if(length == null) {
			this.length = file.length();
		}
		return this.length;
	}


	@Override
	public String getEncoding() {
		return this.encoding;
	}


	/* (non-Javadoc)
	 * @see n3phele.agent.repohandlers.Repo#isExisting()
	 */
	@Override
	public boolean isExisting() {
		return this.file.exists();
	}
	
	


	/* (non-Javadoc)
	 * @see n3phele.agent.repohandlers.Repo#modificationTime()
	 */
	@Override
	public long modificationTime() {
		return this.file.lastModified();
	}


	/* (non-Javadoc)
	 * @see n3phele.agent.repohandlers.Repo#setModificationTime(long)
	 */
	@Override
	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
		
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String result= "File://"+root+"/"+key;
		if(length != null && length != 0 && encoding != null) {
			result += " [";
			String seperate = "";
			if(length != 0) {
				result += "length="+length;
				seperate = " ";
			}
			if(encoding != null) 
				result += seperate+"encoding="+encoding;
			result += "]";
		}
		return result;
	}
	
	public boolean isDirectory() {
		return file.isDirectory();
	}
	
	public Map<String, Long> ls(File dir, String prefix) {
		if(prefix == null || prefix.length() == 0) {
			prefix = "";
		} else if(!prefix.endsWith(File.pathSeparator)) {
			prefix += File.pathSeparator;
		}
		Map<String,Long> result = new HashMap<String,Long>();
		File content[] = dir.listFiles();
		if(content != null) {
			for(File i : content) {
				if(i.isDirectory()) {
					result.putAll(ls(i, prefix+i.getName()+File.pathSeparator));
				} else {
					if(i.isFile()) {
						result.put(i.getPath(),i.length());
					}
				}
			}
		}
		return result;
	}


	@Override
	public List<String> getFileList() {
		List<String> result = new ArrayList<String>();
		int starIndex = key.indexOf("*");
		int questionIndex = key.indexOf("?");
		int curlyIndex = key.indexOf("{");
		if(starIndex == -1 && questionIndex == -1 && curlyIndex == -1) {
			this.totalLength = null;
			return result; // not wildcard
		}
		
		if(starIndex == -1) starIndex = Integer.MAX_VALUE;
		if(questionIndex == -1) questionIndex = Integer.MAX_VALUE;
		if(curlyIndex == -1) curlyIndex = Integer.MAX_VALUE;

		int wildStart = Math.min(Math.min(starIndex, questionIndex), curlyIndex);

		String base = key.substring(0, wildStart);
		base = base.substring(0,base.lastIndexOf(File.pathSeparator)+1);
		String wild = key.substring(base.lastIndexOf(File.pathSeparator)+1);
		this.baseFile = new File(this.root+File.pathSeparator+base);
		Map<String,Long>ls = ls(this.baseFile, null);

		Pattern pattern = Pattern.compile(Helper.wildcardToRegex(wild));
		this.totalLength = 0L;
		for(String f : ls.keySet()) {
			if(pattern.matcher(f).matches()) {
				result.add(f);
				this.totalLength += ls.get(f);
				log.info("Adding "+f+" size "+ls.get(f));
			}
		}
		
		return result;
	}


	@Override
	public void setNextFile(String file) {
		this.file = new File(this.baseFile.getPath()+File.pathSeparator+file);
	}


	@Override
	public long getTotalLength() {
		if(this.totalLength == null)
			this.totalLength = getLength();
		return this.totalLength;
	}
}
