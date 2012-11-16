package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;

public class NettyServerModule extends AbstractModule {

	@Override
	protected void configure() {
	}

	@Provides
	SocketAddress provideSocketAddress() {
		return new InetSocketAddress(8080);
	}

	@Provides
	@Singleton
	ChannelGroup provideChannelGroup() {
		return new DefaultChannelGroup("http-server");
	}

	@Provides
	ChannelFactory provideChannelFactory() {
		return new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors.newCachedThreadPool()
		);
	}
}