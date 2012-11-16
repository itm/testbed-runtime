package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

class NettyServerHandler extends SimpleChannelHandler {

	private final ChannelGroup allChannels;

	@Inject
	NettyServerHandler(ChannelGroup allChannels) {
		this.allChannels = allChannels;
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		allChannels.add(e.getChannel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().close();
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.setContent(ChannelBuffers.copiedBuffer("Hello, world!", Charsets.UTF_8));
		e.getChannel().write(response).addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				future.getChannel().close();
			}
		}
		);
	}

}