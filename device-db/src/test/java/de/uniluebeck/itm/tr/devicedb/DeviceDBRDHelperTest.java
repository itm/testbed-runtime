package de.uniluebeck.itm.tr.devicedb;

import eu.smartsantander.rd.jaxb.Capabilities;
import eu.smartsantander.rd.jaxb.CapabilityType;
import eu.smartsantander.rd.jaxb.ResourceDescription;
import eu.wisebed.wiseml.Capability;
import junit.framework.Assert;
import org.junit.Test;

import static eu.smartsantander.rd.jaxb.IoTNodeType.SENSOR_NODE;

/**
 */
public class DeviceDBRDHelperTest {


	@Test
	public void deviceConfigFromRDResourceWithUnknownUnitTest(){


		final String phenomenon = "SomePhenomenon";

		ResourceDescription rdResource = new ResourceDescription();

		rdResource.setResourceType(SENSOR_NODE);

		rdResource.setUid("urn:wisebed:uzl1:0x2087");
		rdResource.setNodePort(null);

		CapabilityType capabilityType = new CapabilityType();
		capabilityType.setType("float");
		capabilityType.setUom("SomeInvalidUnit");
		capabilityType.setData(true);
		capabilityType.setPhenomenon(phenomenon);
		rdResource.setParentId("testbed.smartsantander.eu");



		Capabilities capabilities = new Capabilities();
		capabilities.getCapability().add(capabilityType);
		rdResource.setCapabilities(capabilities);


		final DeviceConfig deviceConfig = DeviceDBRDHelper.deviceConfigFromRDResource(rdResource);
		final Capability capability = deviceConfig.getCapabilities().iterator().next();


		Assert.assertEquals(phenomenon, capability.getName());

		Assert.assertTrue(capability.getUnit() == null);


	}

	@Test
	public void deviceConfigFromRDResourceWithUnknownDataTypeTest(){

		final String phenomenon = "SomePhenomenon";

		ResourceDescription rdResource = new ResourceDescription();

		rdResource.setResourceType(SENSOR_NODE);

		rdResource.setUid("urn:wisebed:uzl1:0x2087");
		rdResource.setNodePort(null);

		CapabilityType capabilityType = new CapabilityType();
		capabilityType.setType("SomeInvalidType");
		capabilityType.setUom("lumen");
		capabilityType.setData(true);
		capabilityType.setPhenomenon(phenomenon);
		rdResource.setParentId("testbed.smartsantander.eu");



		Capabilities capabilities = new Capabilities();
		capabilities.getCapability().add(capabilityType);
		rdResource.setCapabilities(capabilities);


		final DeviceConfig deviceConfig = DeviceDBRDHelper.deviceConfigFromRDResource(rdResource);
		final Capability capability = deviceConfig.getCapabilities().iterator().next();


		Assert.assertEquals(phenomenon, capability.getName());

		Assert.assertTrue(capability.getDatatype() == null);


	}


}
