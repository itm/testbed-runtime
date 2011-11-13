/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck,                                                *
 * Institute of Operating Systems and Computer Networks Algorithms Group  University of Braunschweig                  *                          *
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
package de.uniluebeck.itm.tr.runtime.socketconnector.server;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.gtr.application.TestbedApplicationFactory;
import de.uniluebeck.itm.tr.runtime.socketconnector.server.xml.Socketconnector;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import static com.google.common.base.Throwables.propagate;


@SuppressWarnings("UnusedDeclaration")
public class SocketConnectorApplicationFactory implements TestbedApplicationFactory {

	public TestbedApplication create(TestbedRuntime testbedRuntime, String applicationName, Object configuration) {
		try {

			JAXBContext context = JAXBContext.newInstance(Socketconnector.class);
			Socketconnector socketConnectorApp =
					(Socketconnector) context.createUnmarshaller().unmarshal((Node) configuration);

			return new SocketConnectorApplication(testbedRuntime, socketConnectorApp.getPort());

		} catch (JAXBException e) {
			throw propagate(e);
		}
	}

}
