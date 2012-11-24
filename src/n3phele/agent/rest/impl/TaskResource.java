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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.core.util.Base64;

import n3phele.agent.Service;
import n3phele.agent.model.CommandRequest;
import n3phele.agent.model.FileRef;
import n3phele.agent.model.Task;
import n3phele.service.core.Resource;

@Path("task")
public class TaskResource {
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(TaskResource.class.getName()); 
	

	@Context UriInfo uriInfo;
	static List<Task> tasks = new ArrayList<Task>();
	@GET
	@Produces("application/json")
	@RolesAllowed("authenticated")
	public List<Task> list() {
		List<Task> result;
		synchronized(tasks) {
			result = new ArrayList<Task>(tasks);
		}
		return result;
	}
	
	@GET
	@Path("date")
	@RolesAllowed("authenticated")
	public String date() {
		if(Service.myURI== null) Service.myURI = uriInfo.getBaseUriBuilder().build();
		return Calendar.getInstance().getTime().toString();
	}
	
	@GET
	@Path("terminate")
	@RolesAllowed("authenticated")
	public Response terminate() {
		Thread t = new Thread() {
		@Override
		public void run() {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			Service.stopServer();
		}};
		t.start();
		return Response.ok().build();
	}
	
	
	
	
	@GET
	@Path("/{id}")
	@Produces("application/json")
	@RolesAllowed("authenticated")
	public Task get(@PathParam("id") String id) {
		List<Task> copy;
		synchronized(tasks) {
			copy = new ArrayList<Task>(tasks);
		}
		for(Task t : copy) {
			if(t.getId().equals(id))
				return t;
		}
		throw new NotFoundException("Task id "+id+" not found");
	}
	
	
	@POST
	@Consumes("application/json")
	@RolesAllowed("authenticated")
	public Response create(CommandRequest cr) {
		String stdin = cr.getStdin();
		URI notificationURI = cr.getNotification();
		String cmd = cr.getCmd();
		FileRef[] files = cr.getFiles();
		if(stdin == null)
			stdin = "";

		Task t = new Task(new String[] {"/bin/bash", "-e", "-c", cmd }, stdin, notificationURI);
		TaskExecuter exe = new TaskExecuter(t.getCmd(), t.getStdin(), t, files);
		if(Service.myURI== null) Service.myURI = uriInfo.getBaseUriBuilder().build();
		t.setUri(uriInfo.getBaseUriBuilder().path(TaskResource.class).path(t.getId()).build());
		synchronized (tasks) {
			tasks.add(t);
		}
		exe.start();

		return Response.created(t.getUri()).build();
	}
	
	
	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Path("/xfer")
	@RolesAllowed("authenticated")
	public Response xfer(@FormParam("source")URI source,
						 @FormParam("tag") String tag,
						 @FormParam("description") String description,
					     @FormParam("srcRoot")String srcRoot,
					     @FormParam("srcKey") String srcKey,
					     @FormParam("srcAccount") String srcAccount,
					     @FormParam("srcSecret")String srcSecret,
					     @FormParam("srcKind") String srcKind,
					     @FormParam("destination")URI destination,
					     @FormParam("destRoot")String destRoot,
					     @FormParam("destKey") String destKey,
					     @FormParam("destAccount") String destAccount,
					     @FormParam("destSecret")String destSecret,
					     @FormParam("destKind") String destKind,
					     @FormParam("notification") String notification,
					     @DefaultValue("false")@FormParam("lazyXfer") boolean lazyXfer) {
		
		URI notificationURI = (notification == null || notification == "") ? null : URI.create(notification);
		Task t = new Task(null, null, notificationURI);
		srcAccount = decryptor(srcAccount, Resource.get("agentSecret", ""));
		srcSecret = decryptor(srcSecret, Resource.get("agentSecret", "")); 
		destAccount = decryptor(destAccount, Resource.get("agentSecret", ""));
		destSecret = decryptor(destSecret, Resource.get("agentSecret", "")); 
		
		XferExecuter exe = new XferExecuter(t, lazyXfer, tag, description, source, srcRoot, srcKey, srcAccount, srcSecret, srcKind,
											destination, destRoot, destKey, destAccount, destSecret, destKind);
		if(Service.myURI== null) Service.myURI = uriInfo.getBaseUriBuilder().build();
		t.setUri(uriInfo.getBaseUriBuilder().path(TaskResource.class).path(t.getId()).build());	
		synchronized (tasks) {
			tasks.add(t);
		}
		exe.start();

		return Response.created(t.getUri()).build();
	}
	
	
	@DELETE
	@Path("{id}")
	@RolesAllowed("authenticated")
	public void delete(@PathParam("id") String id) {
		Task t = get(id);
		if(t.getProcess() != null)
			t.getProcess().destroy();
		synchronized(tasks) {
			tasks.remove(t);
		}
	}
	
	@GET
	@Path("{id}/kill")
	@Produces("application/json")
	@RolesAllowed({"authenticated"})
	public Task kill(@PathParam("id") String id) {
		Task t = get(id);
		delete(id);
		return t;
	}

	
	private String decryptor(String encrypted, String passwd) {
		if(encrypted == null || encrypted == "")
			return "";
		try {
			byte[] key = (passwd).getBytes("UTF-8"); 
			MessageDigest sha = MessageDigest.getInstance("SHA-1"); 
			key = sha.digest(key); 
			key = Arrays.copyOf(key, 16); // use only first 128 bit 
	
			
			SecretKeySpec spec = new SecretKeySpec(key, "AES");

			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, spec);
			return new String(cipher.doFinal(Base64.decode(encrypted)));
		} catch (InvalidKeyException e) {
			//log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (NoSuchAlgorithmException e) {
			//log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (NoSuchPaddingException e) {
			//log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (IllegalBlockSizeException e) {
			//log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (BadPaddingException e) {
			//log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (UnsupportedEncodingException e) {
			//log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		}
		
	}
	

}
