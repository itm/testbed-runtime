package de.uniluebeck.itm.tr.snaa;

import com.google.inject.PrivateModule;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.config.CommonConfig;

public class SNAAServerModule extends PrivateModule {

	private final CommonConfig commonConfig;

	private final SNAAConfig snaaConfig;

	public SNAAServerModule(final CommonConfig commonConfig, final SNAAConfig snaaConfig) {
		this.commonConfig = commonConfig;
		this.snaaConfig = snaaConfig;
	}

	@Override
	protected void configure() {
		install(new ServicePublisherCxfModule());
		install(new SNAAServiceModule(commonConfig, snaaConfig));
	}
}
