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
package n3phele.agent.model;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import n3phele.service.core.Resource;

import com.sun.jersey.core.util.Base64;

// FiXME way way way way too many methods to similarly named


@XmlRootElement(name = "Credential")
@XmlType(name = "Credential", propOrder = { "account", "secret" })
public class Credential {
	final private static Logger log = Logger.getLogger(Credential.class
			.getName());
	
	private String account;
	private String secret;

	public Credential() {
	}

	public Credential(String account, String secret) {
		this.account = account;
		this.secret = secret;
	}

	public Credential decrypt() {
		String password = Resource.get("password", "");
		return decrypt(this, password);
	}
	
	
	public static Credential decrypt(Credential credential) {
		String password = Resource.get("password", "");
		return decrypt(credential, password);
	}

	public static Credential decrypt(Credential credential, String password) {
		Credential result = new Credential(decrypt(credential.getAccount(),
				password), decrypt(credential.getSecret(), password));
		return result;
	}

	private static String decrypt(String encrypted, String passwd) {
		return decryptor(encrypted, "elehp3N" + passwd);
	}

	private static String decryptor(String encrypted, String passwd) {
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
			log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (NoSuchPaddingException e) {
			log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (IllegalBlockSizeException e) {
			log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (BadPaddingException e) {
			log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, "Decryption error", e);
			throw new IllegalArgumentException(e);
		}

	}
	
	public Credential encrypt() {
		String password = Resource.get("password", "");
		return encrypt(this, password);
	}

	public static Credential encrypt(Credential credential) {
		String password = Resource.get("password", "");
		return encrypt(credential, password);
	}

	public static Credential encrypt(Credential credential, String secret) {
		String password = secret;
		Credential result = new Credential(encrypt(credential.getAccount(),
				password), encrypt(credential.getSecret(), password));
		return result;

	}

	private static String encrypt(String str, String passwd) {
		if(str == null) str = "";
		return encryptor(str, "elehp3N" + passwd);
	}

	private static String encryptor(String str, String passwd) {
		try {
			byte[] key = (passwd).getBytes("UTF-8");
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16); // use only first 128 bit
			SecretKeySpec spec = new SecretKeySpec(key, "AES");

			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, spec);
			return new String(Base64.encode(cipher.doFinal(str
					.getBytes("UTF-8"))));
		} catch (InvalidKeyException e) {
			log.log(Level.SEVERE, "Encryption error", e);
			throw new IllegalArgumentException(e);
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "Encryption error", e);
			throw new IllegalArgumentException(e);
		} catch (NoSuchPaddingException e) {
			log.log(Level.SEVERE, "Encryption error", e);
			throw new IllegalArgumentException(e);
		} catch (IllegalBlockSizeException e) {
			log.log(Level.SEVERE, "Encryption error", e);
			throw new IllegalArgumentException(e);
		} catch (BadPaddingException e) {
			log.log(Level.SEVERE, "Encryption error", e);
			throw new IllegalArgumentException(e);
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, "Encryption error", e);
			throw new IllegalArgumentException(e);
		}

	}

	public static Credential unencrypted(Credential encrypted) {
		return decrypt(encrypted);
	}

	public static Credential unencrypted(Credential encrypted, String secret) {
		return decrypt(encrypted, secret);
	}

	public static Credential reencrypt(Credential encrypted, String secret) {
		Credential result = unencrypted(encrypted);
		result.setAccount(encryptor(result.getAccount(), secret));
		result.setSecret(encryptor(result.getSecret(), secret));
		return result;
	}

	public static Credential encrypted(Credential unencrypted) {
		return encrypt(unencrypted);
	}

	/*
	 * Getters and Setters -------------------
	 */

	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * @param account
	 *            the account to set
	 */
	public void setAccount(String account) {
		this.account = account;
	}

	/**
	 * @return the secret
	 */
	public String getSecret() {
		return secret;
	}

	/**
	 * @param secret
	 *            the secret to set
	 */
	public void setSecret(String secret) {
		this.secret = secret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Credential [account=%s, secret=%s]",
				account, secret);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((account == null) ? 0 : account.hashCode());
		result = prime * result + ((secret == null) ? 0 : secret.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Credential other = (Credential) obj;
		if (account == null) {
			if (other.account != null)
				return false;
		} else if (!account.equals(other.account))
			return false;
		if (secret == null) {
			if (other.secret != null)
				return false;
		} else if (!secret.equals(other.secret))
			return false;
		return true;
	}
	
	
}
