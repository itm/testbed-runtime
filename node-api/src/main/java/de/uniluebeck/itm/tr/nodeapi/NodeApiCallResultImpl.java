package de.uniluebeck.itm.tr.nodeapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NodeApiCallResultImpl implements NodeApiCallResult {

	private static final Logger log = LoggerFactory.getLogger(NodeApiCallResult.class);

	private byte responseType;

	private byte[] responsePayload;

	public NodeApiCallResultImpl(final int requestId, byte responseType, byte[] responsePayload) {

		this.responseType = responseType;
		this.responsePayload = responsePayload;

		if (responseType == ResponseType.COMMAND_SUCCESS) {
			log.debug("Invoking callback.success() for request ID {}", requestId);
		} else {
			log.debug("Invoking callback.failure() for request ID {}", requestId);
		}

	}

	@Override
	public boolean isSuccessful() {
		return responseType == ResponseType.COMMAND_SUCCESS;
	}

	@Override
	public byte getResponseType() {
		return responseType;
	}

	@Override
	public byte[] getResponse() {
		return responsePayload;
	}
}
