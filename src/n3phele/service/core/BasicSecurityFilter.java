/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.core;

import com.sun.jersey.api.container.MappableContainerException;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

public class BasicSecurityFilter implements ContainerRequestFilter {

    @Context
    UriInfo uriInfo;
    private static final String REALM = "N3phele authentication";

    public ContainerRequest filter(ContainerRequest request) {
        User user = authenticate(request);
        request.setSecurityContext(new Authorizer(user));
        return request;
    }

    private User authenticate(ContainerRequest request) {
        // Extract authentication credentials
        String authentication = request.getHeaderValue(ContainerRequest.AUTHORIZATION);
        if (authentication == null) {
        	if(_isSecure()) {
	            throw new MappableContainerException
	                    (new AuthenticationException("Authentication credentials are required", REALM));
        	} else {
        		return null;
        	}
        }
        if (!authentication.startsWith("Basic ")) {
            return null;
            // additional checks should be done here
            // "Only HTTP Basic authentication is supported"
        }
        authentication = authentication.substring("Basic ".length());
        String[] values = new String(Base64.base64Decode(authentication)).split(":");
        if (values.length < 2) {
            throw new WebApplicationException(400);
            // "Invalid syntax for username and password"
        }
        String username = values[0];
        String password = values[1];
        if ((username == null) || (password == null)) {
            throw new WebApplicationException(400);
            // "Missing username or password"
        }

        // Validate the extracted credentials
        User user = null;

        if (serviceCredentials.containsKey(username) && password.equals(serviceCredentials.get(username))) {
            user = new User(username, "authenticated");
        /*} else if (username.equals("fred") && password.equals("3hyebbehg56yeh5")) {
            user = new User("fred", "user");*/
        } else {
            throw new MappableContainerException(new AuthenticationException("Invalid username or password", REALM));
        }
        return user;
    }
    private boolean _isSecure() {
    	return "https".equals(uriInfo.getRequestUri().getScheme());
    }
    public class Authorizer implements SecurityContext {

        private User user;
        private Principal principal;

        public Authorizer(final User user) {
            this.user = user;
            this.principal = new Principal() {

                public String getName() {
                    return (user == null)? null : user.username;
                }
            };
        }

        public Principal getUserPrincipal() {
            return this.principal;
        }

        public boolean isUserInRole(String role) {
        	if(user == null) throw new MappableContainerException (new AuthenticationException("Authentication credentials are required", REALM));
            return (role.equals(user.role));
        }

        public boolean isSecure() {
            return _isSecure();
        }

        public String getAuthenticationScheme() {
            return SecurityContext.BASIC_AUTH;
        }
    }

    public class User {

        public String username;
        public String role;

        public User(String username, String role) {
            this.username = username;
            this.role = role;
        }
    }
    
    static Map<String,String> serviceCredentials = new HashMap<String,String>();
    static {
    	String serviceRole= Resource.get("serviceRole", "#");
    	String name = Resource.get(serviceRole+"User", null);
    	String secret = Resource.get(serviceRole+"Secret", null);
    	if(name != null && name != "") {
    		serviceCredentials.put(name, secret);
    	}
    	
    	
    }
    
}