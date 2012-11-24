/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.model;

import java.net.URI;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlRootElement(name="CommandRequest")
@XmlType(name="CommandRequest", propOrder={"cmd", "stdin", "notification", "files"})

public class CommandRequest {
	String cmd;
	String stdin;
	URI notification;
	FileRef[] files;
	
	public CommandRequest() {}

	/**
	 * @return the cmd
	 */
	public String getCmd() {
		return cmd;
	}

	/**
	 * @param cmd the cmd to set
	 */
	public void setCmd(String cmd) {
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
	 * @return the files
	 */
	public FileRef[] getFiles() {
		return files;
	}

	/**
	 * @param files the files to set
	 */
	public void setFiles(FileRef[] files) {
		this.files = files;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format(
				"CommandRequest [cmd=%s, stdin=%s, notification=%s, files=%s]",
				cmd, stdin, notification, files);
	}
	
	
}
