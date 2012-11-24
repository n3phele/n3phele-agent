/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
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

