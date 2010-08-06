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

package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.gtr.application.TestbedApplicationFactory;
import de.uniluebeck.itm.motelist.MoteListLinux;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.WsnDevice;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.Wsnapp;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;


public class WSNDeviceAppFactory implements TestbedApplicationFactory {

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceAppFactory.class);

	@Override
	public TestbedApplication create(TestbedRuntime testbedRuntime, String applicationName, Object configuration) {

		try {

			JAXBContext context = JAXBContext.newInstance(Wsnapp.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			Wsnapp config = (Wsnapp) unmarshaller.unmarshal((Node) configuration);

			ImmutableList.Builder<WSNDeviceApp> builder = new ImmutableList.Builder<WSNDeviceApp>();
			MoteListLinux moteList = null;

			for (WsnDevice wsnDevice : config.getDevice()) {
                StringUtils.assertHexOrDecLongUrnSuffix(wsnDevice.getId());
                
				long id = StringUtils.parseHexOrDecLong(wsnDevice.getId());
				String serialInterface = wsnDevice.getSerialinterface();
				String autodetectionMac = wsnDevice.getAutodetectionMac();
				String type = wsnDevice.getType();
				String urn = wsnDevice.getUrn();

				if (serialInterface == null || "".equals(serialInterface)) {

					log.debug("Using motelist to detect port for {} mote with MAC address {}", type, autodetectionMac);

					if (moteList == null) {
						try {
							moteList = new MoteListLinux();
						} catch (IOException e) {
							log.warn("" + e, e);
							continue;
						}
					}

					serialInterface = moteList.getMotePort(type, StringUtils.parseHexOrDecLong(autodetectionMac));

					if (serialInterface == null) {
						log.warn("No serial interface could be detected for {} mote with MAC address {}", type,
								autodetectionMac
						);
						continue;
					}

				}

				try {

					log.debug("Trying to connect to {} device on port {}", type, serialInterface);

					iSenseDevice device = DeviceFactory.create(type, serialInterface);
					Injector injector = Guice.createInjector(new WSNDeviceAppModule(urn, id, device, testbedRuntime));
					builder.add(injector.getInstance(WSNDeviceApp.class));

				} catch (Exception e) {
					log.error("Exception while starting WSNApp: " + e, e);
				}


			}

			return new WSNDeviceAppWrapper(builder.build());

		} catch (JAXBException e) {
			log.error("Error unmarshalling WsnApplication config: " + e, e);
		}

		return null;

	}

}
