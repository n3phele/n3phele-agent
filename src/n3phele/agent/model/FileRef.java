/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.model;

import java.net.URI;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
;

/** Describes a file in terms of both its canonical name on a local machine as well as location on a
 * remote repository.
 *
 */
@XmlRootElement(name="FileRef")
@XmlType(name="FileRef", propOrder={"name", "description", "source", "kind", "root", "key", "length", "modified", "optional"})
public class FileRef {
	private String name;
	private String description;
	private String source;
	private String root;
	private String key;
	private String kind;
	private long length;
	private Date modified;
	private boolean isOptional;
	private Credential repoCredential = new Credential();
	
	public FileRef() {}
	

	/**
	 * @param name identifying name of the file. The name is unique within a command
	 * @param description of the file contents
	 * @param source repository for the file
	 * @param root root directory for the file in the repository
	 * @param key file key relative to the repository and root
	 * @param repoCredential credential used to access the repository
	 * @param kind the type of the repository
	 * @param length of the file in bytes. 0 if unknown
	 * @param modified date of last file modification. null is unknown
	 * @param isOptional true if the file is not necessarily present
	 */
	public FileRef(String name, String description,
		 URI source, String root, String key, Credential repoCredential, String kind, long length, Date modified, boolean isOptional) {
		this.name = name;
		setDescription(description);
		this.source = (source == null) ? null : source.toString();
		this.root = root;
		this.key = key;
		this.repoCredential = repoCredential;
		this.kind = kind;
		this.length = length;
		this.modified = modified;
	}
	
	public FileRef(FileRef i) {
		this(   i.name,
				i.getDescription(),
				(i.source==null)?null:URI.create(i.source),
				i.root,
				i.key,
				i.repoCredential,
				i.kind,
				i.length,
				i.modified,
				i.isOptional
			);
	}
    
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}


	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}


	/**
	 * @return the source
	 */
	public URI getSource() {
		URI result = null;
		if(this.source != null)
			result = URI.create(this.source);
		return result;
	}


	/**
	 * @param source the source to set
	 */
	public void setSource(URI source) {
		this.source = (source == null)?null : source.toString();
	}


	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}


	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}


	/**
	 * @return the kind
	 */
	public String getKind() {
		return kind;
	}


	/**
	 * @param kind the kind to set
	 */
	public void setKind(String kind) {
		this.kind = kind;
	}


	/**
	 * @return the repoCredential
	 */
	@XmlTransient
	public Credential getRepoCredential() {
		return repoCredential;
	}


	/**
	 * @param repoCredential the repoCredential to set
	 */
	public void setRepoCredential(Credential repoCredential) {
		this.repoCredential = repoCredential;
	}


	/**
	 * @return the root
	 */
	public String getRoot() {
		return root;
	}


	/**
	 * @param root the root to set
	 */
	public void setRoot(String root) {
		this.root = root;
	}


	/**
	 * @return the length
	 */
	public long getLength() {
		return length;
	}


	/**
	 * @param length the length to set
	 */
	public void setLength(long length) {
		this.length = length;
	}


	/**
	 * @return the modified
	 */
	public Date getModified() {
		return modified;
	}


	/**
	 * @param modified the modified to set
	 */
	public void setModified(Date modified) {
		this.modified = modified;
	}


	/**
	 * @return the isOptional
	 */
	public boolean isOptional() {
		return this.isOptional;
	}

	/**
	 * @return the isOptional
	 */
	public boolean getOptional() {
		return this.isOptional;
	}

	/**
	 * @param isOptional the isOptional to set
	 */
	public void setOptional(boolean isOptional) {
		this.isOptional = isOptional;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("FileRef [name=%s, description=%s, source=%s, root=%s, key=%s, kind=%s, length=%s, modified=%s, isOptional=%s, repoCredential=%s]",
						this.name, this.description, this.source, this.root,
						this.key, this.kind, this.length, this.modified,
						this.isOptional, this.repoCredential);
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((this.description == null) ? 0 : this.description.hashCode());
		result = prime * result + (this.isOptional ? 1231 : 1237);
		result = prime * result
				+ ((this.key == null) ? 0 : this.key.hashCode());
		result = prime * result
				+ ((this.kind == null) ? 0 : this.kind.hashCode());
		result = prime * result + (int) (this.length ^ (this.length >>> 32));
		result = prime * result
				+ ((this.modified == null) ? 0 : this.modified.hashCode());
		result = prime * result
				+ ((this.name == null) ? 0 : this.name.hashCode());
		result = prime
				* result
				+ ((this.repoCredential == null) ? 0 : this.repoCredential
						.hashCode());
		result = prime * result
				+ ((this.root == null) ? 0 : this.root.hashCode());
		result = prime * result
				+ ((this.source == null) ? 0 : this.source.hashCode());
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileRef other = (FileRef) obj;
		if (this.description == null) {
			if (other.description != null)
				return false;
		} else if (!this.description.equals(other.description))
			return false;
		if (this.isOptional != other.isOptional)
			return false;
		if (this.key == null) {
			if (other.key != null)
				return false;
		} else if (!this.key.equals(other.key))
			return false;
		if (this.kind == null) {
			if (other.kind != null)
				return false;
		} else if (!this.kind.equals(other.kind))
			return false;
		if (this.length != other.length)
			return false;
		if (this.modified == null) {
			if (other.modified != null)
				return false;
		} else if (!this.modified.equals(other.modified))
			return false;
		if (this.name == null) {
			if (other.name != null)
				return false;
		} else if (!this.name.equals(other.name))
			return false;
		if (this.repoCredential == null) {
			if (other.repoCredential != null)
				return false;
		} else if (!this.repoCredential.equals(other.repoCredential))
			return false;
		if (this.root == null) {
			if (other.root != null)
				return false;
		} else if (!this.root.equals(other.root))
			return false;
		if (this.source == null) {
			if (other.source != null)
				return false;
		} else if (!this.source.equals(other.source))
			return false;
		return true;
	}
	
}
