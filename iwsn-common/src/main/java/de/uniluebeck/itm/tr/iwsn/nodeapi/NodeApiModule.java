package de.uniluebeck.itm.tr.iwsn.nodeapi;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class NodeApiModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder().implement(NodeApi.class, NodeApiImpl.class).build(NodeApiFactory.class));
	}
}
