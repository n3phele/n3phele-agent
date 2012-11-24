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