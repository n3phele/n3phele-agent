/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.repohandlers;

import java.io.DataInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import n3phele.agent.model.FileRef;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;


public class S3 implements Repo {
	private static Logger log = Logger.getLogger(S3.class.getName());
	private AWSCredentials credentials;
	private Long length = null;
	private String encoding;
	private String root;
	private String key;
	private AmazonS3 s3 = null;
	private S3Object object = null;
	private ObjectMetadata objectMetadata = null;
	private Long modificationTime = null;
	private Long totalLength = null;
	private String base = null;
	private URI source;
	private String kind;
	String tag;
	String description;

	public S3(String tag, String description, String accessKey, String secretKey, URI source, String kind, String root, String key) {
		this.credentials = new BasicAWSCredentials(accessKey, secretKey);
		this.root = root;
		this.key = key;
		this.source = source;
		this.kind = kind;
		this.tag = tag;
		this.description = description;
	}
	
	public InputStream getInputStream() throws IOException {	

		 this.encoding = objectMetadata().getContentType();
         log.info("Input: "+source+"/"+root+"/"+key+" Content-Type: "  + this.encoding + "length: " + length );
         return new DataInputStream(object().getObjectContent());
	}
	
	public FileRef put(InputStream input, long length, String encoding) {
		FileRef result = new FileRef(tag, description, source, root, key, null, kind, 0, null, false);
		try {
			objectMetadata = new ObjectMetadata();
			objectMetadata.setContentLength(this.length = length);
			this.encoding = encoding;
			if(encoding != null)
				objectMetadata.setContentType(this.encoding);
	        log.info("Output: "+source+"/"+root+"/"+key+" Content-Type: "  + encoding  + "length: " + length );
			PutObjectResult object = s3().putObject(root, key, input, objectMetadata);
	        result.setLength(length);
	        ObjectMetadata od = s3.getObjectMetadata(root, key);
	        result.setModified(od.getLastModified());
		}  finally {
			try {
				input.close();
			} catch (IOException e) {
			} 
		}
		return result;
    
	}
	
	public boolean isExisting() {
		try {
			getLength();
			return true;	
		} catch (Exception e) {
			return false;
		}
	}
	
	
	
	/* (non-Javadoc)
	 * @see n3phele.agent.repohandlers.Repo#modificationTime()
	 */
	@Override
	public long modificationTime() {
		return objectMetadata().getLastModified().getTime();
	}

	private AmazonS3 s3() {
		if(this.s3 == null) {
			this.s3 = new AmazonS3Client(this.credentials);
			this.s3.setEndpoint(source.toString());
		}
		return this.s3;
	}
	
	
	private S3Object object() {
		S3Object result;
		if(this.object == null) {
			this.object = s3().getObject(new GetObjectRequest(this.root, this.key));
			result = this.object;
		} else {
			result = this.object;
			this.object = null;
		}
		return result;
	}
	
	private ObjectMetadata objectMetadata() {
		if(this.objectMetadata == null) 
			this.objectMetadata = object().getObjectMetadata();
		return this.objectMetadata;
	}
	
	
//	public void get(String bucketName, String key, File file) throws FileNotFoundException {
//
//		
//		AmazonS3 s3 = new AmazonS3Client(this.credentials);
//		 S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
//		 this.length = object.getObjectMetadata().getContentLength();
//		 this.progress = 0;
//         log.info("Content-Type: "  + object.getObjectMetadata().getContentType()+" "+file.getAbsolutePath());
//         
//         DataInputStream reader = new DataInputStream(object.getObjectContent());
//         DataOutputStream writer = new DataOutputStream(new FileOutputStream(file));
//         byte[] buffer = new byte[8*4096];
//    	try {
//    		int len;
//            while ((len = reader.read(buffer)) != -1) {
//				this.progress += len;
//				writer.write(buffer, 0, len);
//            }
//		} catch (IOException e) {
//			log.log(Level.SEVERE, "File "+file.getAbsolutePath(),e);
//		} finally {
//			log.info("written "+this.progress+" bytes");
//			try {
//				writer.close();
//			} catch (IOException e) {
//			} finally {
//				try {
//					reader.close();
//				} catch (IOException e) {
//				}
//			}
//		}
//	}
	/**
	 * @return the length
	 */
	public long getLength() {
		if(this.length == null)
			this.length = object().getObjectMetadata().getContentLength();
		return length;
	}
	
	
	
	/**
	 * @param modificationTime the modificationTime to set
	 */
	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	/**
	 * @return the encoding
	 */
	public String getEncoding() {
		return encoding;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String result= "S3://"+root+"/"+key;
		if(length != null && length != 0 && encoding != null) {
			result += " [";
			String seperate = "";
			if(length != 0) {
				result += "length="+length;
				seperate = " ";
			}
			if(encoding != null) 
				result += seperate+"encoding="+encoding;
			result += "]";
		}
		return result;
	}

	@Override
	public List<String> getFileList() {
		List<String> result = new ArrayList<String>();
		int starIndex = key.indexOf("*");
		int questionIndex = key.indexOf("?");
		int curlyIndex = key.indexOf("{");
		if(starIndex == -1 && questionIndex == -1 && curlyIndex == -1) {
			this.totalLength = null;
			return result; // not wildcard
		}
		
		if(starIndex == -1) starIndex = Integer.MAX_VALUE;
		if(questionIndex == -1) questionIndex = Integer.MAX_VALUE;
		if(curlyIndex == -1) curlyIndex = Integer.MAX_VALUE;

		int wildStart = Math.min(Math.min(starIndex, questionIndex), curlyIndex);

		base = key.substring(0, wildStart);
		base = base.substring(0,base.lastIndexOf("/")+1);
		// String wild = key.substring(base.lastIndexOf("/")+1);
		boolean done = false;
		this.totalLength = 0L;
		Pattern pattern = Pattern.compile("^"+Helper.wildcardToRegex(key));
		ObjectListing listing = s3().listObjects(this.root, base);
		while(!done) {
			done = !listing.isTruncated();

			for(S3ObjectSummary f : listing.getObjectSummaries()) {
				if(pattern.matcher(f.getKey()).matches()) {
					String file = f.getKey().substring(base.length());
					result.add(file);
					this.totalLength += f.getSize();
					log.info("Adding "+file+" size "+f.getSize());
				}
			}
			if(!done)
				listing = s3().listNextBatchOfObjects(listing);
		}
		
		return result;
	}

	@Override
	public void setNextFile(String file) {
		this.key = base + file;	
		if(this.objectMetadata != null)
			this.objectMetadata = null;
		if(this.object != null) {
			try {
				this.object.getObjectContent().close();
			} catch (IOException e) {
				// ignore
			}
		}
		this.object = null;
	}

	@Override
	public long getTotalLength() {
		if(this.totalLength == null)
			this.totalLength = getLength();
		return this.totalLength;
	}
	
	
	
}
