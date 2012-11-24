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
