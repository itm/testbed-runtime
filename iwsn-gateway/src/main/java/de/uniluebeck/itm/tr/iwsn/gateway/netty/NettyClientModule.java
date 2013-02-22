package de.uniluebeck.itm.tr.iwsn.gateway.netty;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;

public class NettyClientModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder().build(NettyClientFactory.class));
	}

	@Provides
	SocketAddress provideSocketAddress() {
		return new InetSocketAddress(8080);
	}

	@Provides
	@Singleton
	ChannelGroup provideChannelGroup() {
		return new DefaultChannelGroup();
	}

	@Provides
	@Singleton
	ChannelFactory provideChannelFactory() {
		return new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()
		);
	}
}