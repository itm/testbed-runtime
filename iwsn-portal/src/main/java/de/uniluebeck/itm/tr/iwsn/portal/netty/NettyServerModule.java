package de.uniluebeck.itm.tr.iwsn.portal.netty;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class NettyServerModule extends AbstractModule {

	private final ThreadFactory bossExecutorThreadFactory;

	private final ThreadFactory workerExecutorThreadFactory;

	public NettyServerModule() {
		this.bossExecutorThreadFactory = Executors.defaultThreadFactory();
		this.workerExecutorThreadFactory = Executors.defaultThreadFactory();
	}

	public NettyServerModule(final ThreadFactory bossExecutorThreadFactory,
							 final ThreadFactory workerExecutorThreadFactory) {
		this.bossExecutorThreadFactory = bossExecutorThreadFactory;
		this.workerExecutorThreadFactory = workerExecutorThreadFactory;
	}

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder().build(NettyServerFactory.class));
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
	ChannelFactory provideChannelFactory() {
		return new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(bossExecutorThreadFactory),
				Executors.newCachedThreadPool(workerExecutorThreadFactory)
		);
	}
}