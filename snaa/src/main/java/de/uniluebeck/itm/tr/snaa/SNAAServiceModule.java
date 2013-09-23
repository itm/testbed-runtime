package de.uniluebeck.itm.tr.snaa;

import com.google.inject.PrivateModule;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.dummy.DummySNAAModule;
import de.uniluebeck.itm.tr.snaa.jaas.JAASSNAAModule;
import de.uniluebeck.itm.tr.snaa.remote.RemoteSNAAModule;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAModule;
import de.uniluebeck.itm.tr.snaa.shiro.JpaModule;
import de.uniluebeck.itm.tr.snaa.shiro.ShiroSNAAModule;
import de.uniluebeck.itm.tr.snaa.certificate.SNAACertificateModule;
import eu.wisebed.api.v3.snaa.SNAA;

public class SNAAServiceModule extends PrivateModule {

	private final CommonConfig commonConfig;

	private final SNAAServiceConfig snaaServiceConfig;

	public SNAAServiceModule(final CommonConfig commonConfig, final SNAAServiceConfig snaaServiceConfig) {
		this.commonConfig = commonConfig;
		this.snaaServiceConfig = snaaServiceConfig;
	}

	@Override
	protected void configure() {
		try {
			requireBinding(CommonConfig.class);
			requireBinding(SNAAServiceConfig.class);
			requireBinding(ServedNodeUrnPrefixesProvider.class);

			switch (snaaServiceConfig.getSnaaType()) {
				case DUMMY:
					install(new DummySNAAModule(snaaServiceConfig));
					break;
				case JAAS:
					install(new JAASSNAAModule(commonConfig, snaaServiceConfig));
					break;
				case SHIBBOLETH:
					install(new ShibbolethSNAAModule(snaaServiceConfig));
					break;
				case SHIRO:
					install(new JpaModule("ShiroSNAA", snaaServiceConfig.getShiroJpaProperties()));
					install(new ShiroSNAAModule(snaaServiceConfig));
					break;
				case CERTIFICATE:
					install(new JpaModule("ShiroSNAA", snaaServiceConfig.getCertificateJpaPropertiesFile()));
					install(new SNAACertificateModule());
					break;
				case REMOTE:
					install(new RemoteSNAAModule());
					break;
				default:
					throw new IllegalArgumentException("Unknown authentication type " + snaaServiceConfig.getSnaaType());
			}

			expose(SNAA.class);
			expose(SNAAService.class);
		} catch (Exception e) {
			addError(e);
		}
	}
}
