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

package n3phele.agent.repohandlers;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import n3phele.agent.model.Origin;

import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.hpcloud.objectstorage.HPCloudObjectStorageClient;
import org.jclouds.openstack.swift.domain.MutableObjectInfoWithMetadata;
import org.jclouds.openstack.swift.domain.SwiftObject;

public class Swift implements Repo {
private static Logger log = Logger.getLogger(Swift.class.getName());
private String accessKey;
private String secretKey;
private Long length = null;
private String encoding;
private String root;
private String key;
private MutableObjectInfoWithMetadata objectMetadata = null;
private Long totalLength = null;
private URI source;
private String kind;
String tag;
String description;
private long modificationTime=0;

	public Swift(String tag, String description, String accessKey, String secretKey, URI source, String kind, String root, String key) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.root = root;
		this.key = key;
		this.source = source;
		this.kind = kind;
		this.tag = tag;
		this.description = description;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		 this.encoding = objectMetadata().getContentType();
         log.info("Input: "+source+"/"+root+"/"+key+" Content-Type: "  + this.encoding + "length: " + length );
         return new DataInputStream(getJcloudsContext().getObject(this.root, this.key).getPayload().getInput());
	}

	@Override
	public Origin put(InputStream input, long length, String encoding)
			throws IOException {
		Origin result = new Origin(source+"/"+root+"/"+key, 0, null, null);
		try {
			HPCloudObjectStorageClient swift = getJcloudsContext();
			SwiftObject objectDefn = swift.newSwiftObject();
			objectDefn.getInfo().setName(key);
			objectDefn.setPayload(input);
			objectDefn.getInfo().setBytes(length);
			objectDefn.getInfo().setContentType(encoding);
			if(modificationTime != 0)
				objectDefn.getInfo().setLastModified(new Date(modificationTime));
			swift.putObject(this.root, objectDefn);
			this.objectMetadata = swift.getObjectInfo(this.root, this.key);
			result.setLength(this.objectMetadata.getBytes());
			result.setModified(this.objectMetadata.getLastModified());
		} finally {
			try {
				input.close();
			} catch (IOException e) {
			} 
		}
		return result;
	}

	@Override
	public long getLength() {
		if(this.length == null)
			this.length = objectMetadata().getBytes();
		return length;
	}

	@Override
	public String getEncoding() {
		return this.encoding;
	}

	@Override
	public boolean isExisting() {
		try {
			getLength();
			return true;	
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public long modificationTime() {
		if(modificationTime == 0) {
			Date lastModified = objectMetadata().getLastModified();
			return this.modificationTime = (lastModified==null?new Date().getTime():lastModified.getTime());
		}
		return this.modificationTime;
	}

	@Override
	/**
	 * @param modificationTime the modificationTime to set
	 */
	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}


	@Override
	public List<String> getFileList() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setNextFile(String file) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getTotalLength() {
		if(this.totalLength == null)
			this.totalLength = getLength();
		return this.totalLength;
	}
	
	private final static Map<String, BlobStoreContext> contextMap = new HashMap<String, BlobStoreContext>();
	private HPCloudObjectStorageClient getJcloudsContext() {

		BlobStoreContext context;
		String cacheKey = this.accessKey+"#"+this.secretKey;
		synchronized(contextMap) {
			context = contextMap.get(cacheKey);
		}
		if(context != null) {
			return HPCloudObjectStorageClient.class.cast(context.getProviderSpecificContext().getApi());
		}


		context = new BlobStoreContextFactory().createContext("hpcloud-objectstorage", this.accessKey, this.secretKey);
		
		HPCloudObjectStorageClient client = HPCloudObjectStorageClient.class.cast(context.getProviderSpecificContext().getApi());
		synchronized(contextMap) {
			contextMap.put(cacheKey, context);
		}
		return client;
	}

	private MutableObjectInfoWithMetadata objectMetadata() {
		if(this.objectMetadata == null) 
			this.objectMetadata = getJcloudsContext().getObjectInfo(this.root, this.key);
		return this.objectMetadata;
	}
}
