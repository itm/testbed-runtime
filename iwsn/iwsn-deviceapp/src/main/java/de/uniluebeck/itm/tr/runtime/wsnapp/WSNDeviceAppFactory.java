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

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplicationFactory;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.Configuration;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.WsnDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;


public class WSNDeviceAppFactory implements TestbedApplicationFactory {

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceAppFactory.class);

	@Override
	public WSNDeviceApp create(TestbedRuntime testbedRuntime, String applicationName, Object xmlConfig) {

		try {

			JAXBContext context = JAXBContext.newInstance(WsnDevice.class.getPackage().getName());
			Unmarshaller unmarshaller = context.createUnmarshaller();

			WsnDevice wsnDevice = (WsnDevice) ((JAXBElement) unmarshaller.unmarshal((Node) xmlConfig)).getValue();

			try {

				Map<String, String> configuration = convert(wsnDevice.getConfiguration());

				WSNDeviceAppConfiguration.Builder builder = WSNDeviceAppConfiguration
						.builder(wsnDevice.getUrn(), wsnDevice.getType())
						.setNodeSerialInterface(wsnDevice.getSerialinterface())
						.setMaximumMessageRate(wsnDevice.getMaximummessagerate())
						.setNodeUSBChipID(wsnDevice.getUsbchipid());

				if (wsnDevice.getTimeouts() != null) {
					builder.setTimeoutFlash(wsnDevice.getTimeouts().getFlash());
					builder.setTimeoutNodeAPI(wsnDevice.getTimeouts().getNodeapi());
					builder.setTimeoutReset(wsnDevice.getTimeouts().getReset());
				}

				if (wsnDevice.getConfiguration() != null) {
					builder.setConfiguration(configuration);
				}

				return new WSNDeviceAppImpl(testbedRuntime, builder.build());

			} catch (Exception e) {
				throw propagate(e);
			}

		} catch (JAXBException e) {
			throw propagate(e);
		}
	}

	@Nullable
	private Map<String, String> convert(@Nullable final List<Configuration> configuration) {

		if (configuration == null) {
			return null;
		}

		Map<String, String> map = newHashMap();
		for (Configuration configurationEntry : configuration) {
			map.put(configurationEntry.getKey(), configurationEntry.getValue());
		}
		return map;
	}

}
