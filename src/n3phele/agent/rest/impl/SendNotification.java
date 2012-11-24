/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.rest.impl;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import n3phele.agent.Service;
import n3phele.agent.model.Task;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class SendNotification {
	private static Logger log = Logger.getLogger(SendNotification.class.getName());
	private static Client client = Client.create();
	public static void sendCompletionNotification(Task t) {
		URI notification = t.getNotification();
		log.info("SendNotification to <"+notification+"> from "+t.getCmd()+" on "+Service.myURI);

		if(notification == null)
			return;
		
		pause(1500);
		
		WebResource resource = client.resource(notification);

		ClientResponse response = resource.queryParam("source", Service.myURI.toString())
											.queryParam("oldStatus", "RUNNING")
											.queryParam("newStatus", t.getExitcode()==0?"COMPLETE":"FAILED")
											.queryParam("reference", UUID.randomUUID().toString())
											.queryParam("sequence", "0")
											.type(MediaType.TEXT_PLAIN)
											.get(ClientResponse.class);
		log.info("Notificaion Status "+response.getStatus());
	}
	
	private static void pause(final long t) {
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
			log.log(Level.WARNING, "Interrrupted", e);
		}
	}

}
