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

package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.inject.Guice;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.gtr.application.TestbedApplicationFactory;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.Portalapp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


public class PortalServerFactory implements TestbedApplicationFactory {

	private static final Logger log = LoggerFactory.getLogger(PortalServerFactory.class);

	@Override
	public TestbedApplication create(TestbedRuntime testbedRuntime, String applicationName, Object configuration) {

		try {

			JAXBContext context = JAXBContext.newInstance(Portalapp.class);
			Portalapp portalapp = (Portalapp) context.createUnmarshaller().unmarshal((Node) configuration);
			String sessionManagementEndpointUrl = portalapp.getWebservice().getSessionmanagementendpointurl();
			String wsnInstanceBaseUrl = portalapp.getWebservice().getWsninstancebaseurl();
			String reservationEndpointUrl = portalapp.getWebservice().getReservationendpointurl();
			String urnPrefix = portalapp.getWebservice().getUrnprefix();

			String wiseMLFilename = portalapp.getWebservice().getWisemlfilename();
			File wiseMLFile = new File(wiseMLFilename);

			if (!wiseMLFile.exists()) {
				throw new Exception("WiseML file " + wiseMLFilename + " does not exist!");
			} else if (wiseMLFile.isDirectory()) {
				throw new Exception("WiseML file name " + wiseMLFilename + " points to a directory!");
			} else if (!wiseMLFile.canRead()) {
				throw new Exception("WiseML file " + wiseMLFilename + " can't be read!");
			}

			BufferedReader wiseMLFileReader = new BufferedReader(new FileReader(wiseMLFile));
			StringBuilder wiseMLBuilder = new StringBuilder();
			while (wiseMLFileReader.ready()) {
				wiseMLBuilder.append(wiseMLFileReader.readLine());
			}

			WSNApp wsnApp = Guice.createInjector(new WSNAppModule(testbedRuntime)).getInstance(WSNApp.class);

			SessionManagementService sessionManagementService = Guice.createInjector(
					new PortalModule(
							urnPrefix, sessionManagementEndpointUrl, wsnInstanceBaseUrl,
							reservationEndpointUrl, wsnApp, wiseMLBuilder.toString(),
                            testbedRuntime)
					).getInstance(SessionManagementService.class);

			PortalServerApplication portalServerApplication = new PortalServerApplication(sessionManagementService);

			log.debug("Successfully created PortalServerApplication!");

			return portalServerApplication;

		} catch (JAXBException e) {
			log.error("Exception while unmarshalling PortalServerApplication configuration: " + e, e);
		} catch (Exception e) {
			log.error("" + e, e);
		}

		return null;
	}

}
