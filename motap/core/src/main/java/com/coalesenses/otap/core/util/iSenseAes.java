/**********************************************************************************************************************
 * Copyright (c) 2010, coalesenses GmbH                                                                               *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the coalesenses GmbH nor the names of its contributors may be used to endorse or promote     *
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package com.coalesenses.otap.core.util;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

public class iSenseAes {

	private static final Logger log = LoggerFactory.getLogger(iSenseAes.class);

	KeyParameter keyParameter = null;

	CCMBlockCipher in2;

	long nonce = (int) (Math.random() * 100);

	int macLen = 4;

	public void setKey(byte[] key) {
		Security.addProvider(new BouncyCastleProvider());

		BlockCipher c = new AESEngine();
		in2 = new CCMBlockCipher(c);

		keyParameter = new KeyParameter(key);
	}

	public byte[] encode(byte[] plainText) {
		nonce += (int) (Math.random() * 100);
		//log.debug("nonce="+nonce);
		//nonce = 0x12345678;
		byte[] buffer = new byte[plainText.length + 8];
		// AEAD Parameters are, Key, MAC length in bits, Nonce, and Associated text
		byte[] n = new byte[13];
		n[0] = 0;
		n[1] = (byte) ((nonce >> 24) & 0xFF);
		n[2] = (byte) ((nonce >> 16) & 0xFF);
		n[3] = (byte) ((nonce >> 8) & 0xFF);
		n[4] = (byte) ((nonce) & 0xFF);
		n[5] = (byte) ((nonce >> 24) & 0xFF);
		n[6] = (byte) ((nonce >> 16) & 0xFF);
		n[7] = (byte) ((nonce >> 8) & 0xFF);
		n[8] = (byte) ((nonce) & 0xFF);
		n[9] = (byte) ((nonce >> 24) & 0xFF);
		n[10] = (byte) ((nonce >> 16) & 0xFF);
		n[11] = (byte) ((nonce >> 8) & 0xFF);
		n[12] = (byte) ((nonce) & 0xFF);

		AEADParameters params = new AEADParameters(keyParameter, macLen * 8, n, null);
		//true for encryption mode
		in2.init(true, params);

		//do the encryption and authentication, input, input offset (8 bytes are Authenticated data), length of text to encryption, out put buffer, output offset
		in2.processBytes(plainText, 0, plainText.length, buffer, 0);
		try {
			in2.doFinal(buffer, 0);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidCipherTextException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		buffer[buffer.length - 4] = n[1];
		buffer[buffer.length - 3] = n[2];
		buffer[buffer.length - 2] = n[3];
		buffer[buffer.length - 1] = n[4];

		return buffer;
	}

	public byte[] decode(byte[] cypherText) {
		byte[] n = new byte[13];
		byte[] buffer = new byte[cypherText.length - 4 - macLen];
		n[0] = 0;
		System.arraycopy(cypherText, cypherText.length - 4, n, 1, 4);
		System.arraycopy(cypherText, cypherText.length - 4, n, 5, 4);
		System.arraycopy(cypherText, cypherText.length - 4, n, 9, 4);

		AEADParameters params = new AEADParameters(keyParameter, macLen * 8, n, null);
		//true for encryption mode
		in2.init(false, params);
		in2.processBytes(cypherText, 0, cypherText.length - 4, buffer, 0);

		try {
			in2.doFinal(buffer, 0);
		} catch (IllegalStateException e) {

			// TODO Auto-generated catch block
			log.warn("illegal state");
			in2.reset();
			return null;
		} catch (InvalidCipherTextException e) {
			log.warn("invalid cipher");
			// TODO Auto-generated catch block
			//e.printStackTrace();
			in2.reset();
			return null;
		}

		return buffer;
	}
}
