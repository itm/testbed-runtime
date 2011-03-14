/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
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

package de.uniluebeck.itm.tr.runtime.wsperf;

import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v22.BinaryMessage;
import eu.wisebed.testbed.api.wsn.v22.Message;
import eu.wisebed.testbed.api.wsn.v22.WSN;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.ws.Endpoint;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * User: bimschas Date: 17.05.2010 Time: 20:54:32
 */
public class WSPerformanceTest {

	public static void main(String[] args) throws DatatypeConfigurationException, MalformedURLException {

		String wsnEndpointUrl = "http://localhost:8080/wsn/";

		WSNImpl wsnImpl = new WSNImpl();

		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(wsnEndpointUrl);

		System.out.println("Starting WSPerformanceTest service...");
		System.out.println("Endpoint URL: " + wsnEndpointUrl);
		System.out.println("Binding  URL: " + bindAllInterfacesUrl);

		Endpoint endpoint = Endpoint.publish(bindAllInterfacesUrl, wsnImpl);
		endpoint.setExecutor(Executors.newCachedThreadPool());

		System.out.println("Started WSPerformanceTest on " + bindAllInterfacesUrl);

		WSN wsn = WSNServiceHelper.getWSNService(wsnEndpointUrl);

		List<String> nodeUrns = Arrays.asList("urn:wisebed:testbeduzl1:n1");

		long before = System.currentTimeMillis();

		for (int i = 0; i < 1000; i++) {

			BinaryMessage binaryMessage = new BinaryMessage();
			binaryMessage.setBinaryType((byte) 0);
			binaryMessage.setBinaryData(new byte[]{0x00});

			Message message = new Message();
			message.setSourceNodeId("urn:wisebed:testbeduzl1:n2");
			message.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(
					(GregorianCalendar) GregorianCalendar.getInstance()));
			message.setBinaryMessage(binaryMessage);

			wsn.send(nodeUrns, message);
		}

		long after = System.currentTimeMillis();

		System.out.println("Overall duration: " + (after - before) + " ms");
		System.out.println("Duration per invocation: " + ((after - before) / 1000) + " ms");

		endpoint.stop();

	}

}
