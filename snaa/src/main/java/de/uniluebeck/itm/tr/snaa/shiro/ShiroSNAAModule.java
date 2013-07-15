package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import eu.wisebed.api.v3.snaa.SNAA;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.subject.Subject;

import javax.persistence.EntityManager;

/**
 * Extension of the abstract {@link ShiroModule} to configure Apache Shiro and to bind dependencies
 * for {@link ShiroSNAA}.
 */
public class ShiroSNAAModule extends ShiroModule {

	private final SNAAServiceConfig snaaServiceConfig;

	public ShiroSNAAModule(final SNAAServiceConfig snaaServiceConfig) {
		this.snaaServiceConfig = snaaServiceConfig;
	}

	@Override
	protected void configureShiro() {
		try {

			requireBinding(CommonConfig.class);
			requireBinding(SNAAServiceConfig.class);
			requireBinding(EntityManager.class);
			requireBinding(ServicePublisher.class);
			requireBinding(ServedNodeUrnPrefixesProvider.class);

			bind(CredentialsMatcher.class).to(HashedCredentialsMatcher.class);
			bind(HashedCredentialsMatcher.class); // needs to be bound explicitly to have the shiro TypeListener work
			bindConstant().annotatedWith(Names.named("shiro.hashAlgorithmName"))
					.to(snaaServiceConfig.getShiroHashAlgorithmName());
			bindConstant().annotatedWith(Names.named("shiro.hashIterations"))
					.to(snaaServiceConfig.getShiroHashAlgorithmIterations());

			bindRealm().to(ShiroSNAAJPARealm.class).in(Singleton.class);

			bind(ShiroSNAA.class).in(Scopes.SINGLETON);
			bind(SNAA.class).to(ShiroSNAA.class);
			bind(SNAAService.class).to(ShiroSNAA.class);

			expose(ShiroSNAA.class);
			expose(SNAA.class);
			expose(SNAAService.class);

		} catch (Exception e) {
			addError(e);
		}
	}

	@Provides
	private Subject provideCurrentUser() {
		return SecurityUtils.getSubject();
	}
}