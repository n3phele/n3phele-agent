/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent;

import java.io.IOException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;

import n3phele.agent.rest.impl.TaskResource;
import n3phele.service.core.BasicSecurityFilter;

import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class Service {
    private static HttpServer webServer;
    public static URI myURI = null;
   
    protected static void startServer() throws IllegalArgumentException, NullPointerException, IOException {
    	DefaultResourceConfig resourceConfig = new DefaultResourceConfig(TaskResource.class);   
    	resourceConfig.getContainerRequestFilters().add(new BasicSecurityFilter());
    	resourceConfig.getResourceFilterFactories().add(new RolesAllowedResourceFilterFactory());

        webServer = GrizzlyServerFactory.createHttpServer("http://0.0.0.0:8887", resourceConfig);

    }

    public static void stopServer() {
        webServer.stop();
        System.exit(0);
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 try {
			startServer();
			 while(true) {
		    	 try {
					Thread.sleep(10000L);
				} catch (InterruptedException e) {
				}
		     }
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NullPointerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    
	}

}
