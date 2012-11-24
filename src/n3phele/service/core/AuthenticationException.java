/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.core;

public class AuthenticationException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public AuthenticationException(String message, String realm) {
        super(message);
        this.realm = realm;
    }
    private String realm = null;
    public String getRealm() {
        return this.realm;
    }
}
