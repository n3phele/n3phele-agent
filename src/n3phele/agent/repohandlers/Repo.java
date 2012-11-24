/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.repohandlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import n3phele.agent.model.FileRef;

public interface Repo {

	public abstract InputStream getInputStream()
			throws IOException;

	public abstract FileRef put(InputStream input,
			long length, String encoding) throws IOException;
	
	/**
	 * @return the length
	 */
	public long getLength();
	
	/**
	 * @return the encoding
	 */
	public String getEncoding() ;

	public boolean isExisting();

	public long modificationTime();

	public void setModificationTime(long srcModificationTime);

	public List<String> getFileList() throws Exception;

	public void setNextFile(String file) throws Exception;

	public long getTotalLength();
	

}