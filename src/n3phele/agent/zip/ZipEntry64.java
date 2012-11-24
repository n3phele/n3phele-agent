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
package n3phele.agent.zip;

import java.util.zip.ZipEntry;

public class ZipEntry64 extends java.util.zip.ZipEntry {
	long size = -1;
	public ZipEntry64(ZipEntry64 e) {
		super(e);
	}


	public ZipEntry64(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}


	/* (non-Javadoc)
	 * @see java.util.zip.ZipEntry#getSize()
	 */
	@Override
	public long getSize() {
		return size;
	}


	/* (non-Javadoc)
	 * @see java.util.zip.ZipEntry#setSize(long)
	 */
	@Override
	public void setSize(long size) {
        if (size < 0) {  
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = size;

	}


}
