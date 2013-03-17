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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import n3phele.agent.model.Task;

public class TaskExecuter extends Thread {
	private static Logger log = Logger.getLogger(TaskExecuter.class.getName());

	String[] cmd;
	String stdin;
	Task me;
	Map<String, String> myEnv;
	static AtomicLong idSeed = new AtomicLong(1);
	
	TaskExecuter(String[] cmd, String stdin, Task me) {
		this(cmd, stdin, me, null);
	}
	
	TaskExecuter(String[] cmd, String stdin, Task me, Map<String,String> myEnv) {
		this.cmd = cmd;
		this.stdin = stdin;
		this.me = me;
		if(myEnv == null) {
			myEnv = new HashMap<String, String>();
		}
		this.myEnv = myEnv;

		me.setId(Long.toString(idSeed.getAndIncrement()));
	}
	
	public void run() {
		try {            
			String concat = "TaskExecution: ";
			for(String s : cmd) {
				concat += " "+s;
			}
			log.warning(concat);
			ProcessBuilder pb = new ProcessBuilder(cmd);
			Map<String, String> env = pb.environment();
			env.putAll(myEnv);
			// pb.directory();
			// FIXME set sandbox
			Process proc = pb.start();
			
			
			
			me.setProcess(proc);
			this.setName("Process"+getId());
			
			StreamPump inputPump = new
			StreamPump(proc.getOutputStream(), me.getStdin());
			// any error message?
			StreamGobbler errorGobbler = new 
			StreamGobbler(proc.getErrorStream(), me.getStderrStringBuilder());            

			// any output?
			StreamGobbler outputGobbler = new 
			StreamGobbler(proc.getInputStream(), me.getStdoutStringBuilder());

			// kick them off
			inputPump.start();
			errorGobbler.start();
			outputGobbler.start();

			// any error???
			me.setExitcode(proc.waitFor());
        
		} catch (Throwable t) {
			me.getStderrStringBuilder().append("Exception: "+t.toString()+"\n");
			log.log(Level.WARNING, "Task exception "+this.cmd[0], t);
		} finally {
			me.setFinished(Calendar.getInstance().getTime());
			SendNotification.sendCompletionNotification(me);
		}
	}
	
}
