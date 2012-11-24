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
