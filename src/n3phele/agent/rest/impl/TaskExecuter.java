/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.rest.impl;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import n3phele.agent.model.FileRef;
import n3phele.agent.model.Task;

public class TaskExecuter extends Thread {
	private static Logger log = Logger.getLogger(TaskExecuter.class.getName());

	String[] cmd;
	String stdin;
	Task me;
	Map<String, String> myEnv;
	FileRef[] files;
	static AtomicLong idSeed = new AtomicLong(1);
	
	TaskExecuter(String[] cmd, String stdin, Task me, FileRef[] files) {
		this(cmd, stdin, me, null, files);
	}
	
	TaskExecuter(String[] cmd, String stdin, Task me, Map<String,String> myEnv, FileRef[] files) {
		this.cmd = cmd;
		this.stdin = stdin;
		this.me = me;
		if(myEnv == null) {
			myEnv = new HashMap<String, String>();
		}
		this.myEnv = myEnv;
		this.files = files;
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
			if(this.files != null) {
				for(FileRef file : files) {
					if("File".equals(file.getKind())) {
						String root = file.getRoot();
						String key = file.getKey();
						if(root == null || root == "") {
							root = ".";
						}
						if(key == null)
							key = "";
						try {
							File local = new File(root+File.separator+key);
							file.setLength(local.length());
							file.setModified(new Date(local.lastModified()));
						} catch (Exception e) {
							// ignore
						}
					}
				}
			}
			me.setManifest(files);
			SendNotification.sendCompletionNotification(me);
		}
	}
	
}
