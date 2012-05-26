package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.WSN;

import java.util.List;
import java.util.concurrent.Callable;

public class GetSupportedChannelHandlersCallable implements Callable<List<ChannelHandlerDescription>> {

	private final WSN endpoint;

	public GetSupportedChannelHandlersCallable(WSN endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public List<ChannelHandlerDescription> call() throws Exception {
		return endpoint.getSupportedChannelHandlers();
	}

}
