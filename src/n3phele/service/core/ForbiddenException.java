/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.core;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@SuppressWarnings("serial")
public class ForbiddenException extends WebApplicationException {
	/**
     * Create a HTTP 403 (Forbidden) exception
     */
    public ForbiddenException() {
        super(Response.status(Status.FORBIDDEN).build());
    }

    /**
     * Create a HTTP 403 (Forbidden) exception.
     * @param message the String that is the entity of the 403 response.
     */
    public ForbiddenException(String message) {
        super(Response.status(Status.FORBIDDEN).
                entity(message).type("text/plain").build());
    }
}
