package de.uniluebeck.itm.tr.snaa.shiro;

import org.apache.shiro.crypto.hash.SimpleHash;

import static de.uniluebeck.itm.tr.snaa.shiro.ShiroSNAAModule.DEFAULT_HASH_ALGORITHM_NAME;
import static de.uniluebeck.itm.tr.snaa.shiro.ShiroSNAAModule.DEFAULT_HASH_ITERATIONS;

public class ShiroSNAAPasswordHash {

	public static void main(String[] args) {

		String password = args[0];
		String salt = args[1];
		String hash = new SimpleHash(DEFAULT_HASH_ALGORITHM_NAME, password, salt, DEFAULT_HASH_ITERATIONS).toHex();

		System.out.println("password = " + password);
		System.out.println("salt     = " + salt);
		System.out.println("hash     = " + hash);
	}

}
