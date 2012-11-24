/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.rest.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamContentGobbler extends Thread {
	private InputStream is;
    private StringBuilder result;
    private String encoding;
    private long length;
    private boolean headersProcessed = false;
    private boolean done = false;
    
    public StreamContentGobbler(InputStream is, StringBuilder result) {
        this.is = is;
        this.result = result;
    }
    
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null) {
                if(line.startsWith("encoding=")) {
                	encoding = line.substring("encoding=".length());
                } else if(line.startsWith("length=")) {
                	length = Long.valueOf(line.substring("length=".length()));
                	headersProcessed = true;
                	break;
                } else {
                	result.append(line+"\n");
                	break;
                }
            }
 
            while ( (line = br.readLine()) != null)
                result.append(line+"\n");
        } catch (IOException ioe) {
        } finally {
        	done = true;
        }
    
    	
    }

	/**
	 * @return the encoding
	 */
	public String getEncoding() {
		return encoding;
	}


	/**
	 * @return the length
	 */
	public long getLength() {
		return length;
	}

	/**
	 * @return the headersProcessed
	 */
	public boolean isHeadersProcessed() {
		return headersProcessed;
	}

	/**
	 * @return the done
	 */
	public boolean isDone() {
		return done;
	}


}
