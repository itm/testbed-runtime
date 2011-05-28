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

package eu.wisebed.testbed.api.rs;

import de.uniluebeck.itm.tr.util.FileUtils;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.RSService;

import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class RSServiceHelper {

	private static File tmpFileRS = null;

	private static final Lock tmpFileRSLock = new ReentrantLock();

	/**
	 * Returns the port to the RS API.
	 *
	 * @param endpointUrl the endpoint URL to connect to
	 * @return a {@link eu.wisebed.api.rs.RS} instance that is
	 *         connected to the Web Service endpoint
	 */
	public static RS getRSService(String endpointUrl) {

		InputStream resourceStream = RSServiceHelper.class.getClassLoader().getResourceAsStream("RSService.wsdl");

		tmpFileRSLock.lock();
		try {
			if (tmpFileRS == null) {
				try {
					tmpFileRS = FileUtils.copyToTmpFile(resourceStream, "tr.rs", "wsdl");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			tmpFileRSLock.unlock();
		}

		RSService service;
		try {
			service = new RSService(tmpFileRS.toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		RS rsPort = service.getRSPort();

		Map<String, Object> ctxt = ((BindingProvider) rsPort).getRequestContext();
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		return rsPort;

	}

}
