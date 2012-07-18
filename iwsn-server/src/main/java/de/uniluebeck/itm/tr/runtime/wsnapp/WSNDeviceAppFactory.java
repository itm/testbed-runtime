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

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.application.TestbedApplicationFactory;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.Configuration;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.WsnDevice;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryModule;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.util.FilePreconditions.checkFileExists;
import static de.uniluebeck.itm.tr.util.FilePreconditions.checkFileReadable;


public class WSNDeviceAppFactory implements TestbedApplicationFactory {

	@Override
	public WSNDeviceApp create(TestbedRuntime testbedRuntime, String applicationName, Object xmlConfig) {

		try {

			JAXBContext context = JAXBContext.newInstance(WsnDevice.class.getPackage().getName());
			Unmarshaller unmarshaller = context.createUnmarshaller();

			WsnDevice wsnDevice = (WsnDevice) ((JAXBElement) unmarshaller.unmarshal((Node) xmlConfig)).getValue();

			try {

				final WSNDeviceAppModule wsnDeviceAppModule = new WSNDeviceAppModule();
				final DeviceFactoryModule deviceFactoryModule = new DeviceFactoryModule();
				final Injector injector = Guice.createInjector(wsnDeviceAppModule, deviceFactoryModule);

				WSNDeviceAppConfiguration configuration = createConfiguration(wsnDevice);
				WSNDeviceAppConnectorConfiguration connectorConfiguration = createConnectorConfiguration(wsnDevice);

				DeviceFactory deviceFactory = injector.getInstance(DeviceFactory.class);

				return injector
						.getInstance(WSNDeviceAppGuiceFactory.class)
						.create(testbedRuntime, deviceFactory, configuration, connectorConfiguration);

			} catch (Exception e) {
				throw propagate(e);
			}

		} catch (JAXBException e) {
			throw propagate(e);
		}
	}

	private WSNDeviceAppConnectorConfiguration createConnectorConfiguration(final WsnDevice wsnDevice)
			throws Exception {

		File defaultChannelPipelineConfigurationFile = null;
		if (wsnDevice.getDefaultChannelPipeline() != null) {

			String configurationFileName = wsnDevice.getDefaultChannelPipeline().getConfigurationFile();
			Object configurationXml = wsnDevice.getDefaultChannelPipeline().getConfigurationXml();

			if (configurationFileName != null) {

				checkFileExists(configurationFileName);
				checkFileReadable(configurationFileName);

				defaultChannelPipelineConfigurationFile = new File(configurationFileName);

			} else if (configurationXml != null) {

				defaultChannelPipelineConfigurationFile = File.createTempFile("tr.iwsn-", "");
				defaultChannelPipelineConfigurationFile.deleteOnExit();

				Node firstChild = ((Node) configurationXml).getFirstChild();

				DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
				DOMImplementationLS impl =
						(DOMImplementationLS) registry.getDOMImplementation("LS");
				LSSerializer writer = impl.createLSSerializer();
				writer.writeToURI(firstChild, defaultChannelPipelineConfigurationFile.toURI().toString());

			} else {
				throw new RuntimeException("The default channel pipeline configuration for node \"" +
						wsnDevice.getUrn() + "\" is missing either the configuration file or the xml."
				);
			}
		}

		final Integer maximumMessageRate = wsnDevice.getMaximummessagerate();
		final Integer timeoutCheckAliveMillis =
				wsnDevice.getTimeouts() != null ? wsnDevice.getTimeouts().getCheckalive() : null;
		final Integer timeoutFlashMillis = wsnDevice.getTimeouts() != null ? wsnDevice.getTimeouts().getFlash() : null;
		final Integer timeoutNodeApiMillis =
				wsnDevice.getTimeouts() != null ? wsnDevice.getTimeouts().getNodeapi() : null;
		final Integer timeoutResetMillis = wsnDevice.getTimeouts() != null ? wsnDevice.getTimeouts().getReset() : null;

		return new WSNDeviceAppConnectorConfiguration(
				wsnDevice.getUrn(),
				wsnDevice.getType(),
				wsnDevice.getSerialinterface(),
				wsnDevice.getUsbchipid(),
				convertDeviceConfiguration(wsnDevice.getConfiguration()),
				defaultChannelPipelineConfigurationFile,
				maximumMessageRate,
				timeoutCheckAliveMillis,
				timeoutFlashMillis,
				timeoutNodeApiMillis,
				timeoutResetMillis
		);
	}

	private WSNDeviceAppConfiguration createConfiguration(final WsnDevice wsnDevice) throws Exception {

		return new WSNDeviceAppConfiguration(
				wsnDevice.getUrn(),
				wsnDevice.getDefaultImage() != null ? new File(wsnDevice.getDefaultImage()) : null
		);
	}

	@Nullable
	private Map<String, String> convertDeviceConfiguration(@Nullable final List<Configuration> configuration) {

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
