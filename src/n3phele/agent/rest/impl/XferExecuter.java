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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;

import com.amazonaws.services.s3.internal.Mimetypes;

import n3phele.agent.model.FileRef;
import n3phele.agent.model.Task;
import n3phele.agent.repohandlers.LocalFile;
import n3phele.agent.repohandlers.Repo;
import n3phele.agent.repohandlers.S3Large;
import n3phele.agent.repohandlers.Swift;
import n3phele.agent.zip.ZipInputStream;

public class XferExecuter extends Thread {
	private static Logger log = Logger.getLogger(XferExecuter.class.getName());

	private Task me;
	private String tag;
	private String description;
	private URI source;
	private String srcRoot;
	private String srcKey;
	private String srcAccount;
	private String srcSecret;
	private String srcKind;
	private URI destination; 
	private String destRoot; 
	private String destKey;
	private String destAccount;
	private String destSecret;
	private String destKind;
	@SuppressWarnings("unused")
	private boolean lazyXfer;
	private int retry = 2;
	private List<FileRef> delivered = new ArrayList<FileRef>();

	/** Transfers files between source and destination repositories.
	 * Supports transfer of:
	 * <li> single file from source to destination
	 * <li> one element to a zip file on the source to the destination. This is specified by the source
	 * file containing a string  <i>something</i>.zip/<i>zip_component_path</i>. For example
	 * foo.zip/component/file.txt specifies the file names "component/file.txt" is to be extracted from
	 * foo.zip and transfered to the destination.
	 * <li> transfer of a zip file at the source to an expanded directory of files on the destination. This
	 * is specified by a source file which ends in .zip with a destination which does not end in .zip. So
	 * source of "foo.zip" and destination of "something/bar" would unpack the content of foo.zip into the
	 * something/bar directory on the destination.
	 * @param me
	 * @param lazyXfer
	 * @param tag
	 * @param description
	 * @param source
	 * @param srcRoot
	 * @param srcKey
	 * @param srcAccount
	 * @param srcSecret
	 * @param srcKind
	 * @param destination
	 * @param destRoot
	 * @param destKey
	 * @param destAccount
	 * @param destSecret
	 * @param destKind
	 */
	public XferExecuter(Task me, boolean lazyXfer, String tag, String description,
			URI source,  String srcRoot, String srcKey,
			String srcAccount, String srcSecret, String srcKind,
			URI destination, String destRoot, String destKey,
			String destAccount, String destSecret, String destKind) {
		super();
		this.me = me;
		this.lazyXfer = lazyXfer;
		this.tag = tag;
		this.description = description;
		this.source = source;
		this.srcRoot = srcRoot;
		this.srcKey = srcKey;
		this.srcAccount = srcAccount;
		this.srcSecret = srcSecret;
		this.srcKind = srcKind;
		this.destination = destination;
		this.destRoot = destRoot;
		this.destKey = destKey;
		this.destAccount = destAccount;
		this.destSecret = destSecret;
		this.destKind = destKind;
		this.me.setId(Long.toString(TaskExecuter.idSeed.getAndIncrement()));
	}
	
	/* 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		Repo srcRepository = null;
		Repo destRepository = null;
		
		try {    
			this.setName("Xfer"+getId());
			boolean zipTarget = this.destKey.endsWith("zip");
			boolean zipSrc = this.srcKey.endsWith(".zip");
			int extract = this.srcKey.indexOf(".zip/");
			
			if(extract > 0) {
				ZipInputStream zip=null;
				InputStream extractor=null;
				InputStream progress=null;
				InputStream buffer=null;
				InputStream input=null;
				try {

					// do a file extraction
					String zipComponent = this.srcKey.substring(extract+5); // no leading slash
					String zipFile = this.srcKey.substring(0, extract+4); // no trailing slash
					srcRepository = getRepo(this.tag, this.description, this.srcAccount, this.srcSecret, this.source, this.srcKind, this.srcRoot, zipFile);
					destRepository = getRepo(this.tag, this.description, this.destAccount, this.destSecret, this.destination, this.destKind, this.destRoot, this.destKey);
					me.setCmd(new String[] {srcRepository.toString(), " extracting ", zipComponent, "->", destRepository.toString()});
					log.info(srcRepository.toString()+ " extracting "+zipComponent+"->"+destRepository.toString());

					FileRef ref = destRepository.put(
						extractor = new ZipExtractorStream( zipComponent,
							zip = new ZipInputStream(
								progress = new ProgressInputStream(
									buffer = new BufferedInputStream(input = srcRepository.getInputStream(), 64 *1024), 
									me, 
									srcRepository.getTotalLength()))
						),
							0, null);
					delivered.add(ref);
				} finally {
					try {
						if(zip!=null)
							zip.close();
					} catch (Exception e) {	
					}
					try {
						if(extractor!=null)
							extractor.close();
					} catch (Exception e) {	
					}
					try {
						if(progress!=null)progress.close();
					} catch (Exception e) {	
					}
					try {
						if(buffer!=null)buffer.close();
					} catch (Exception e) {	
					}
					try {
						if(input!=null)input.close();
					} catch (Exception e) {	
					}
				}
			} else if(zipSrc && !zipTarget) {
				// unpack at destination
				ProgressInputStream in = null;
				ZipInputStream zip =null;
				InputStream input=null;
				try {
					srcRepository = getRepo(this.tag, this.description, this.srcAccount, this.srcSecret, this.source, this.srcKind, this.srcRoot, this.srcKey);
	
					in = new ProgressInputStream(input = srcRepository.getInputStream(), me, srcRepository.getTotalLength());
					zip = new ZipInputStream(in);
					for(ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry() ){
						String mimetype = getMimeType(entry.getName());
						log.info("Next entry is "+entry.getName()+" size "+entry.getSize()+" type "+mimetype);
						if(entry.isDirectory()) continue;
						String filename = combinePathFragments(this.destKey,entry.getName());
						destRepository = getRepo(this.tag+"/", this.description, this.destAccount, this.destSecret, this.destination, this.destKind, this.destRoot, 
								filename);
						me.setCmd(new String[] {srcRepository.toString(),"inflating",entry.getName(), "->", destRepository.toString()});
						log.info(srcRepository.toString()+" inflating "+entry.getName()+"->"+destRepository.toString());
						FileRef file = destRepository.put(
								new ProgressInputStream(zip, null, entry.getSize()),
								entry.getSize(), mimetype);
						delivered.add(file);
					}
				} finally {
					try {
						if(zip!=null) zip.close();
					} catch (Exception e) {
						//
					}
					try {
						if(in!=null) in.close();
					} catch (Exception e) {
						//
					}
					try {
						if(input!=null) input.close();
					} catch (Exception e) {
						//
					}
				}
			} else {
				InputStream progress=null ;
				InputStream buffer = null;
				InputStream input=null;
				try {
					srcRepository = getRepo(this.tag, this.description, this.srcAccount, this.srcSecret, this.source, this.srcKind, this.srcRoot, this.srcKey);
					destRepository = getRepo(this.tag, this.description, this.destAccount, this.destSecret, this.destination, this.destKind, this.destRoot, this.destKey);
					long srcModificationTime = srcRepository.modificationTime();
					destRepository.setModificationTime(srcModificationTime);
					
					me.setCmd(new String[] {srcRepository.toString(), "->", destRepository.toString()});
					log.info(srcRepository.toString()+"->"+destRepository.toString());
					FileRef file = destRepository.put(
							progress = new ProgressInputStream(
									buffer = new BufferedInputStream(input = srcRepository.getInputStream(), 64*1024), me, srcRepository.getTotalLength()),
							srcRepository.getLength(), srcRepository.getEncoding());
					delivered.add(file);
				} finally {
					try {
						if(buffer != null) buffer.close();
					} catch (Exception e) {
						//
					}
					try {
						if(progress != null) progress.close();
					} catch (Exception e) {
						//
					}
					try {
						if(input != null) input.close();
					} catch (Exception e) {
						//
					}
				}
			}
			me.setExitcode(0);
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Xfer exception", t);
			me.getStderrStringBuilder().append("Exception: "+t.toString()+"\n");
			if(retry-- > 0) {
				this.delivered.clear();
				this.run();
				me.getStderrStringBuilder().append("Retrying... \n");
			}
		} finally {
			me.setProgress(1000);
			me.setFinished(Calendar.getInstance().getTime());
			me.setManifest(this.delivered.toArray(new FileRef[this.delivered.size()]));
			try {
				srcRepository.getInputStream().close();
			} catch (Exception e) {
				// ignore;
			}
			SendNotification.sendCompletionNotification(me);
		}
	}
	
	private Repo getRepo(String tag, String description, String account, String secret,
			URI uri, String kind, String root, String key)  {
		Repo repo;
		if("S3".equals(kind)) {
			repo = new S3Large(tag, description, account, secret, uri, kind,
					root, key);
		} else if ("File".equals(kind)){
			repo = new LocalFile(tag, description, account, secret, uri, kind,
					root, key);
		} else if ("Swift".equals(kind)){
			repo = new Swift(tag, description, account, secret, uri, kind,
					root, key);
		} else {
			throw new IllegalArgumentException("Unknown kind "+kind);
		}
		return repo;
		
	}
	
	private String getMimeType(String filename) {
        return Mimetypes.getInstance().getMimetype(filename);
	}
	
	private String combinePathFragments(String a, String b) {
		String result;
		if(a==null || a.trim().length()==0) {
			result = "";
		} else {
			result = a.trim();
			if(!a.endsWith("/")) {
				result += "/";
			}
		}
		if(b!=null) {
			result += b.trim();
		}
		return result;
	}
}



