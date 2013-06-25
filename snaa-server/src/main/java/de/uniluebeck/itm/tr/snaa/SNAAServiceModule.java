package de.uniluebeck.itm.tr.snaa;

import com.google.inject.PrivateModule;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.dummy.DummySNAAModule;
import de.uniluebeck.itm.tr.snaa.jaas.JAASSNAAModule;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAModule;
import de.uniluebeck.itm.tr.snaa.shiro.ShiroSNAAModule;
import eu.wisebed.api.v3.snaa.SNAA;

public class SNAAServiceModule extends PrivateModule {

	private final CommonConfig commonConfig;

	private final SNAAConfig snaaConfig;

	public SNAAServiceModule(final CommonConfig commonConfig, final SNAAConfig snaaConfig) {
		this.commonConfig = commonConfig;
		this.snaaConfig = snaaConfig;
	}

	@Override
	protected void configure() {

		requireBinding(CommonConfig.class);
		requireBinding(SNAAConfig.class);

		switch (snaaConfig.getSnaaType()) {
			case DUMMY:
				install(new DummySNAAModule(snaaConfig));
				break;
			case JAAS:
				install(new JAASSNAAModule(commonConfig, snaaConfig));
				break;
			case SHIBBOLETH:
				install(new ShibbolethSNAAModule(commonConfig, snaaConfig));
				break;
			case SHIRO:
				install(new ShiroSNAAModule(commonConfig, snaaConfig));
				break;
			default:
				throw new IllegalArgumentException("Unknown authentication type " + snaaConfig.getSnaaType());
		}

		expose(SNAA.class);
		expose(SNAAService.class);
	}
}
