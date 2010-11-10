package de.uniluebeck.itm.tr.runtime.wsnapp;


public class WSNDeviceAppConnectorFactory {


	public static WSNDeviceAppConnector create(final String nodeUrn, final String nodeType, final String nodeUSBChipID,
											   final String nodeSerialInterface, final Integer nodeAPITimeout) {

		if ("isense".equals(nodeType) || "telosb".equals(nodeType) || "pacemate".equals(nodeType)) {
			return new WSNDeviceAppConnectorLocal(nodeUrn, nodeType, nodeUSBChipID, nodeSerialInterface, nodeAPITimeout
			);
		} else if ("isense-motap".equals(nodeType)) {
			return new WSNDeviceAppConnectorRemote(nodeUrn, nodeType, nodeAPITimeout);
		}

		throw new RuntimeException("Unknown device type!");
	}
}
