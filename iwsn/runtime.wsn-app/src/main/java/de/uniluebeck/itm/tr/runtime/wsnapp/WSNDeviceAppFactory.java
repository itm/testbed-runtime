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

import com.google.common.base.Preconditions;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplicationFactory;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.WsnDevice;
import de.uniluebeck.itm.tr.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class WSNDeviceAppFactory implements TestbedApplicationFactory {

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceAppFactory.class);

	@Override
	public WSNDeviceApp create(TestbedRuntime testbedRuntime, String applicationName, Object configuration) {

		try {

			JAXBContext context = JAXBContext.newInstance(WsnDevice.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();

			WsnDevice wsnDevice = (WsnDevice) unmarshaller.unmarshal((Node) configuration);

			String nodeUrn, nodeType, nodeSerialInterface, nodeUSBChipID;
			Integer timeoutNodeAPI, maximumMessageRate, timeoutReset, timeoutFlash;

			try {

				nodeUrn = wsnDevice.getUrn();
				Preconditions.checkNotNull(nodeUrn);
				StringUtils.assertHexOrDecLongUrnSuffix(nodeUrn);

				nodeType = wsnDevice.getType();
				Preconditions.checkNotNull(nodeType);

				nodeSerialInterface = wsnDevice.getSerialinterface();
				nodeUSBChipID = wsnDevice.getUsbchipid();
				maximumMessageRate = wsnDevice.getMaximummessagerate();

				timeoutNodeAPI = wsnDevice.getTimeouts() == null ? null : wsnDevice.getTimeouts().getNodeapi();
				Preconditions.checkArgument(
						(timeoutNodeAPI == null || timeoutNodeAPI > 0),
						"The timeout value for the Node API must either be omitted (null) to use the default value "
								+ "or larger than 0 (zero). Current value: " + timeoutNodeAPI
				);

				timeoutReset = wsnDevice.getTimeouts() == null ? null : wsnDevice.getTimeouts().getReset();
				Preconditions.checkArgument(
						(timeoutNodeAPI == null || timeoutNodeAPI > 0),
						"The timeout value for the reset operation must either be omitted (null) to use the default "
								+ "value or larger than 0 (zero). Current value: " + timeoutNodeAPI
				);

				timeoutFlash = wsnDevice.getTimeouts() == null ? null : wsnDevice.getTimeouts().getFlash();
				Preconditions.checkArgument(
						(timeoutNodeAPI == null || timeoutNodeAPI > 0),
						"The timeout value for the flash operation must either be omitted (null) to use the default "
								+ "value or larger than 0 (zero). Current value: " + timeoutNodeAPI
				);

			} catch (Exception e) {
				// ignore this device as it is badly configured
				log.error("Ignoring device. Reason: " + e.getMessage(), e);
				throw new RuntimeException(e);
			}

			return new WSNDeviceAppImpl(
					nodeUrn,
					nodeType,
					nodeSerialInterface,
					timeoutNodeAPI,
					nodeUSBChipID,
					maximumMessageRate,
					testbedRuntime,
					timeoutReset,
					timeoutFlash
			);

		} catch (JAXBException e) {
			log.error("Error unmarshalling WsnApplication config: " + e, e);
		}

		return null;

	}

}
