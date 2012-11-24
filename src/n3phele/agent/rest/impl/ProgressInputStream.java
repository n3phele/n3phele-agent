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

import java.io.IOException; 
import java.io.InputStream; 

import n3phele.agent.model.Task;
 
public class ProgressInputStream extends InputStream { 
    private final Task task; 
    private final InputStream inputStream; 
    private long progress;
    private final long length;
 
    public ProgressInputStream(InputStream inputStream, Task t, long length) { 
        this.inputStream = inputStream;
        this.length = length;
        this.task = t;
        this.progress = 0;
    }  
     
 
    @Override 
    public int read() throws IOException { 
        int count = inputStream.read(); 
        if (count > 0) {
            progress += count;
            if(progress > length)
            	progress = length+1;
            if(task != null)
            	task.setProgress((int)(progress*1000/length));
        }
        return count; 
    } 
    @Override 
    public int read(byte[] b, int off, int len) throws IOException { 
        int count = inputStream.read(b, off, len); 
        if (count > 0) {
            progress += count; 
            if(progress > length)
            	progress = length+1;
            if(task != null)
            	task.setProgress((int)(progress*1000/length));
        }
        return count; 
    }
    
    @Override
    public void close() throws IOException {
    	super.close();
    }

	/**
	 * @return the progress
	 */
	public long getProgress() {
		return progress;
	} 
	
	public InputStream getInputStream() {
		return inputStream;
	}
    
}

