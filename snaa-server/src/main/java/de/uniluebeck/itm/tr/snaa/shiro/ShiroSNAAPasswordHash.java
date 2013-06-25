package de.uniluebeck.itm.tr.snaa.shiro;

import edu.vt.middleware.crypt.digest.SHA512;
import org.apache.shiro.crypto.hash.SimpleHash;

public class ShiroSNAAPasswordHash {

	public static void main(String[] args) {

		String password = args[0];
		String salt = args[1];
		String hash = new SimpleHash(SHA512.ALGORITHM, password, salt, 1000).toHex();

		System.out.println("password = " + password);
		System.out.println("salt     = " + salt);
		System.out.println("hash     = " + hash);
	}

}
