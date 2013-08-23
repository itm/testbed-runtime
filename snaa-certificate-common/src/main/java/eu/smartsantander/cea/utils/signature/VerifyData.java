/*******************************************************************************
 * Copyright (c) 2013 CEA LIST.
 * Contributor:
 *   ROUX Pierre
 *   Kim Thuat NGUYEN
 *******************************************************************************/
package eu.smartsantander.cea.utils.signature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;


public class VerifyData {

	private static final Logger log = LoggerFactory.getLogger(VerifyData.class);

	private static String algorithm = "MD5withRSA";

	public VerifyData(String algorithm) {
		this.algorithm = algorithm;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}


	public static boolean verifySignature(byte[] data, PublicKey pubkey, byte[] signedData) {
		try {
			Signature signer;
			signer = Signature.getInstance(algorithm);
			signer.initVerify(pubkey);
			signer.update(data);
			return signer.verify(signedData);
		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage(),e);
		} catch (InvalidKeyException e) {
			log.error(e.getMessage(),e);
		} catch (SignatureException e) {
			log.error(e.getMessage(),e);
		}

		return false;
	}
}
