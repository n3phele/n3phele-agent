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
package n3phele.agent.model;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;



@XmlRootElement(name="Task")
@XmlType(name="Task", propOrder={"id", "uri", "notification", "started", "finished", "cmd", "stdin", "stdout", "stderr", "exitcode", "progress", "manifest"})
public class Task {
	private String id;
	private URI uri;
	private Date started;
	private Date finished;
	private String[] cmd;
	private String stdin;
	private StringBuilder stdout;
	private StringBuilder stderr;
	private Origin[] manifest;
	private int exitcode;
	private int progress= 0; // 0.. 1000
	private Process process;
	private URI notification;

	
	public Task() {}

	public Task(String[] cmd, String stdin, URI notification) {
		this(null, Calendar.getInstance().getTime(), null, cmd, stdin, new StringBuilder(), new StringBuilder(),
				-1, null, notification);
	}

	/**
	 * @param id
	 * @param RUNNING
	 * @param finished
	 * @param cmd
	 * @param stdin
	 * @param stdout
	 * @param stderr
	 * @param exitcode
	 * @param process
	 */
	public Task(String id, Date started, Date finished, String[] cmd,
			String stdin, StringBuilder stdout, StringBuilder stderr,
			int exitcode, Process process, URI notification) {
		super();
		this.id = id;
		this.started = started;
		this.finished = finished;
		this.cmd = cmd;
		this.stdin = stdin;
		this.stdout = stdout;
		this.stderr = stderr;
		this.exitcode = exitcode;
		this.process = process;
		this.progress = 0;
		this.notification = notification;
	}


	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 * @return the RUNNING
	 */
	public Date getStarted() {
		return started;
	}


	/**
	 * @param RUNNING the RUNNING to set
	 */
	public void setStarted(Date started) {
		this.started = started;
	}


	/**
	 * @return the finished
	 */
	public Date getFinished() {
		return finished;
	}


	/**
	 * @param finished the finished to set
	 */
	public void setFinished(Date finished) {
		this.finished = finished;
	}


	/**
	 * @return the cmd
	 */
	public String[] getCmd() {
		return cmd;
	}


	/**
	 * @param cmd the cmd to set
	 */
	public void setCmd(String[] cmd) {
		this.cmd = cmd;
	}


	/**
	 * @return the stdin
	 */
	public String getStdin() {
		return stdin;
	}


	/**
	 * @param stdin the stdin to set
	 */
	public void setStdin(String stdin) {
		this.stdin = stdin;
	}


	/**
	 * @return the stdout
	 */
	@XmlTransient
	public StringBuilder getStdoutStringBuilder() {
		return stdout;
	}


	/**
	 * @param stdout the stdout to set
	 */
	public void setStdoutStringBuilder(StringBuilder stdout) {
		this.stdout = stdout;
	}


	/**
	 * @return the stderr
	 */
	@XmlTransient
	public StringBuilder getStderrStringBuilder() {
		return stderr;
	}


	/**
	 * @param stderr the stderr to set
	 */
	public void setStderrStringBuilder(StringBuilder stderr) {
		this.stderr = stderr;
	}


	/**
	 * @return the stdout
	 */
	@XmlElement
	public String getStdout() {
		return stdout.toString();
	}

	/**
	 * @param stdout the stdout to set
	 */
	public void setStdout(String stdout) {
		this.stdout = new StringBuilder(stdout);
	}

	/**
	 * @return the stderr
	 */
	@XmlElement
	public String getStderr() {
		return stderr.toString();
	}

	/**
	 * @param stderr the stderr to set
	 */
	public void setStderr(String stderr) {
		this.stderr = new StringBuilder(stderr);
	}

	/**
	 * @return the exitcode
	 */
	public int getExitcode() {
		return exitcode;
	}


	/**
	 * @param exitcode the exitcode to set
	 */
	public void setExitcode(int exitcode) {
		this.exitcode = exitcode;
	}


	/**
	 * @return the process
	 */
	@XmlTransient
	public Process getProcess() {
		return process;
	}


	/**
	 * @param process the process to set
	 */
	public void setProcess(Process process) {
		this.process = process;
	}
	


	/**
	 * @return the progress
	 */
	public int getProgress() {
		return progress;
	}

	/**
	 * @param progress the progress to set
	 */
	public void setProgress(int progress) {
		this.progress = progress;
	}

	/**
	 * @return the uri
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(URI uri) {
		this.uri = uri;
	}
	
	

	/**
	 * @return the notification
	 */
	public URI getNotification() {
		return notification;
	}

	/**
	 * @param notification the notification to set
	 */
	public void setNotification(URI notification) {
		this.notification = notification;
	}

	/**
	 * @return the manifest
	 */
	public Origin[] getManifest() {
		return this.manifest;
	}

	/**
	 * @param manifest the manifest to set
	 */
	public void setManifest(Origin[] manifest) {
		this.manifest = manifest;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("Task [id=%s, uri=%s, started=%s, finished=%s, cmd=%s, stdin=%s, stdout=%s, stderr=%s, manifest=%s, exitcode=%s, progress=%s, process=%s, notification=%s]",
						this.id, this.uri, this.started, this.finished,
						this.cmd, this.stdin, this.stdout, this.stderr,
						this.manifest, this.exitcode, this.progress,
						this.process, this.notification);
	}

}

