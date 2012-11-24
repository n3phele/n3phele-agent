/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.rest.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StreamPump extends Thread
{
	private static Logger log = Logger.getLogger(StreamPump.class.getName());
    OutputStream os;
    String source;
    
    StreamPump(OutputStream os, String source) {
        this.os = os;
        this.source = source;
    }
    
    public void run() {
        try {
            OutputStreamWriter osr = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osr);
            bw.write(source);
            log.warning("Pump: "+source);
            bw.close();
        } catch (IOException ioe) {
        	log.log(Level.SEVERE, "Pump exception", ioe);
        }
    }

}