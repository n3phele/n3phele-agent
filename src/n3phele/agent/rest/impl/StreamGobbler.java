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
import java.util.logging.Level;
import java.util.logging.Logger;

public class StreamGobbler extends Thread
{
	private static Logger log = Logger.getLogger(StreamGobbler.class.getName());
    InputStream is;
    StringBuilder result;
    
    public StreamGobbler(InputStream is, StringBuilder result) {
        this.is = is;
        this.result = result;
    }
    
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
 
            while ( (line = br.readLine()) != null) {
                log.warning("Gobble: "+line);
                result.append(line+"\n");
            }
        } catch (IOException ioe) {
        	log.log(Level.SEVERE, "Exception ", ioe);
        }
    }

}
