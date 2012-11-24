/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.core;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {
    public Response toResponse(AuthenticationException e) {
        if (e.getRealm() != null) {
            return Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic realm=\"" + e.getRealm() + "\"").type("text/plain").entity(e.getMessage()).build();
        } else {
            return Response.status(Status.UNAUTHORIZED).type("text/plain").entity(e.getMessage()).build();
        }
    }

}
