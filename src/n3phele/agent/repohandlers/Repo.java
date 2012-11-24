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