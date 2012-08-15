package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import eu.wisebed.api.v3.controller.RequestStatus;
import eu.wisebed.api.v3.controller.Status;

/**
 * Helper class for this package that converts types from WSNApp representation to Web service representation and back.
 */
class TypeConverter {

	static RequestStatus convert(WSNAppMessages.RequestStatus requestStatus, long requestId) {
		RequestStatus retRequestStatus = new RequestStatus();
		retRequestStatus.setRequestId(requestId);
		WSNAppMessages.RequestStatus.Status status = requestStatus.getStatus();
		Status retStatus = new Status();
		retStatus.setMsg(status.getMsg());
		retStatus.setNodeUrn(status.getNodeId());
		retStatus.setValue(status.getValue());
		retRequestStatus.getStatus().add(retStatus);
		return retRequestStatus;
	}
}
