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
package n3phele.agent.rest.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;
import n3phele.agent.zip.ZipInputStream;

public class ZipExtractorStream extends InputStream { 
    private final String filter; 
    private ZipEntry current = null;
    private final ZipInputStream inputStream;
 
    public ZipExtractorStream(String filter, ZipInputStream inputStream) { 
    	this.inputStream = inputStream;
        this.filter = filter;

    }  
  
    @Override 
    public int read() throws IOException { 
    	if(!findZipFile(filter)) throw new FileNotFoundException(filter);
        int count = inputStream.read(); 
        return count; 
    } 
    
    @Override 
    public int read(byte[] b, int off, int len) throws IOException { 
    	if(!findZipFile(filter)) throw new FileNotFoundException(filter);
        int count = inputStream.read(b, off, len); 
        return count; 
    }

    private boolean findZipFile(String name) throws IOException {
    	while(this.current == null) {
    		ZipEntry next = inputStream.getNextEntry();
    		if(next == null) return false;
    		if(next.getName().equals(filter)) {
    			current = next;
    		}
    	}
    	return true;
    }
    
    public ZipInputStream getZipInputStream() { return inputStream; }
    
}

