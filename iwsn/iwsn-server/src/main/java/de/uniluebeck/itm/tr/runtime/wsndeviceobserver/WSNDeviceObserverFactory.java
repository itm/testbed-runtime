package de.uniluebeck.itm.tr.runtime.wsndeviceobserver;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.gtr.application.TestbedApplicationFactory;
import de.uniluebeck.itm.tr.runtime.wsndeviceobserver.config.Configuration;
import de.uniluebeck.itm.tr.runtime.wsndeviceobserver.config.Mapping;
import de.uniluebeck.itm.wsn.deviceutils.macreader.DeviceMacReferenceMap;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;

public class WSNDeviceObserverFactory implements TestbedApplicationFactory {

	@Override
	public TestbedApplication create(final TestbedRuntime testbedRuntime, final String applicationName,
									 final Object configuration)
			throws Exception {

		return new WSNDeviceObserver(testbedRuntime, applicationName, parseConfiguration(configuration));
	}

	private WSNDeviceObserverConfiguration parseConfiguration(final Object configurationObject) {
		try {

			JAXBContext context = JAXBContext.newInstance(
					de.uniluebeck.itm.tr.runtime.wsndeviceobserver.config.WSNDeviceObserverConfiguration
							.class.getPackage().getName()
			);
			Unmarshaller unmarshaller = context.createUnmarshaller();

			de.uniluebeck.itm.tr.runtime.wsndeviceobserver.config.WSNDeviceObserverConfiguration xmlConfiguration =
					(de.uniluebeck.itm.tr.runtime.wsndeviceobserver.config.WSNDeviceObserverConfiguration)
							((JAXBElement) unmarshaller.unmarshal((Node) configurationObject)).getValue();

			Map<String, String> properties = newHashMap();
			for (Configuration keyValuePair : xmlConfiguration.getConfiguration()) {
				properties.put(keyValuePair.getKey(), keyValuePair.getValue());
			}

			DeviceMacReferenceMap deviceMacReferenceMap = new DeviceMacReferenceMap();
			for (Mapping mapping : xmlConfiguration.getMapping()) {
				deviceMacReferenceMap.put(
						mapping.getUsbchipid(),
						new MacAddress(mapping.getMac())
				);
			}

			return new WSNDeviceObserverConfiguration(deviceMacReferenceMap, properties);

		} catch (JAXBException e) {
			throw propagate(e);
		}
	}
}
