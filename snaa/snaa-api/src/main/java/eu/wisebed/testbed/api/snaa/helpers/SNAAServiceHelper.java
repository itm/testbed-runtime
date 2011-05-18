/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

package eu.wisebed.testbed.api.snaa.helpers;

import de.uniluebeck.itm.tr.util.FileUtils;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.SNAAService;

import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SNAAServiceHelper {

	private static File tmpFileSNAA = null;

	private static final Lock tmpFileSNAALock = new ReentrantLock();

	/**
	 * Returns the port to the SNAA API.
	 *
	 * @param endpointUrl the endpoint URL to connect to
	 * @return a {@link eu.wisebed.api.snaa.SNAA} instance that is
	 *         connected to the Web Service endpoint
	 */
	public static SNAA getSNAAService(String endpointUrl) {

		InputStream resourceStream = SNAAServiceHelper.class.getClassLoader().getResourceAsStream("SNAAService.wsdl");

		tmpFileSNAALock.lock();
		try {
			if (tmpFileSNAA == null) {
				try {
					tmpFileSNAA = FileUtils.copyToTmpFile(resourceStream, "tr.snaa", "wsdl");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			tmpFileSNAALock.unlock();
		}

		SNAAService service;
		try {
			service = new SNAAService(tmpFileSNAA.toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		SNAA snaaPort = service.getSNAAPort();

		Map<String, Object> ctxt = ((BindingProvider) snaaPort).getRequestContext();
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		return snaaPort;
	}
}
