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
