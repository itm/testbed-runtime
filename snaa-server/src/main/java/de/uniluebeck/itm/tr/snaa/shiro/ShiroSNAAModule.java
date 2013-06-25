package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.subject.Subject;

import javax.persistence.EntityManager;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.inject.util.Providers.of;

/**
 * Extension of the abstract {@link ShiroModule} to configure Apache Shiro and to bind dependencies
 * for {@link ShiroSNAA}.
 */
public class ShiroSNAAModule extends ShiroModule {

	private final CommonConfig commonConfig;

	private final SNAAConfig snaaConfig;

	public ShiroSNAAModule(final CommonConfig commonConfig, final SNAAConfig snaaConfig) {
		this.commonConfig = commonConfig;
		this.snaaConfig = snaaConfig;
	}

	@Override
	protected void configureShiro() {
		try {

			bind(CommonConfig.class).toProvider(of(commonConfig));
			bind(SNAAConfig.class).toProvider(of(snaaConfig));

			install(new JpaModule("ShiroSNAA", snaaConfig.getShiroJpaProperties()));

			requireBinding(EntityManager.class);
			requireBinding(ServicePublisher.class);

			bind(ServedNodeUrnPrefixesProvider.class).toInstance(new ServedNodeUrnPrefixesProvider() {
				@Override
				public Set<NodeUrnPrefix> get() {
					return newHashSet(commonConfig.getUrnPrefix());
				}
			}
			);

			bind(CredentialsMatcher.class).to(HashedCredentialsMatcher.class);
			bind(HashedCredentialsMatcher.class); // needs to be bound explicitly to have the shiro TypeListener work
			bindConstant().annotatedWith(Names.named("shiro.hashAlgorithmName"))
					.to(snaaConfig.getShiroHashAlgorithmName());
			bindConstant().annotatedWith(Names.named("shiro.hashIterations"))
					.to(snaaConfig.getShiroHashAlgorithmIterations());

			bindRealm().to(ShiroSNAAJPARealm.class).in(Singleton.class);

			bind(ShiroSNAA.class).in(Scopes.SINGLETON);
			bind(SNAA.class).to(ShiroSNAA.class);
			bind(SNAAService.class).to(ShiroSNAA.class);

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