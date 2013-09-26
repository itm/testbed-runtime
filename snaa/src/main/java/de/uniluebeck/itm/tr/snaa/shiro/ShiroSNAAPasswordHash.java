package de.uniluebeck.itm.tr.snaa.shiro;

import edu.vt.middleware.crypt.digest.SHA512;
import org.apache.shiro.crypto.hash.SimpleHash;

import java.util.Scanner;

public class ShiroSNAAPasswordHash {

	public static void main(String[] args) {

		String password;
		String salt;
		String hash;

		if (args.length == 0){

			Scanner scanner = new Scanner(System.in);
			System.out.println("Please provide password and salt:");
			System.out.print("Password: ");
			password = scanner.next();

			System.out.print("Salt: ");
			salt = scanner.next();
			hash = new SimpleHash(SHA512.ALGORITHM, password, salt, 1000).toHex();

		}else {
			password = args[0];
			salt = args[1];
			hash = new SimpleHash(SHA512.ALGORITHM, password, salt, 1000).toHex();
		}

		System.out.println("Password = " + password);
		System.out.println("Salt     = " + salt);
		System.out.println("Hash     = " + hash);
	}

}
