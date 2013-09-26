package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.Guice;
import com.google.inject.name.Names;
import com.google.inject.persist.jpa.JpaPersistModule;
import de.uniluebeck.itm.tr.common.jpa.JPAInitializer;
import edu.vt.middleware.crypt.digest.SHA512;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.Properties;

import static com.google.common.base.Throwables.propagate;

/**
 * For external usage as e.g., in a web container such as tomcat for auth* operations.
 */
public class ShiroSNAARealm extends AuthorizingRealm {

	private static final Logger log = LoggerFactory.getLogger(ShiroSNAARealm.class);

	private String jpaProperties;

	private ShiroSNAAJPARealm realmImpl;

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
		log.trace("ShiroSNAARealm.doGetAuthorizationInfo()");
		return realmImpl.doGetAuthorizationInfo(principals);
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token)
			throws AuthenticationException {
		log.trace("ShiroSNAARealm.doGetAuthenticationInfo()");
		return realmImpl.doGetAuthenticationInfo(token);
	}

	@Override
	protected void onInit() {

		log.trace("ShiroSNAARealm.onInit()");
		super.onInit();

		if (jpaProperties == null || "".equals(jpaProperties)) {
			throw new RuntimeException("Configuration parameter \"realm.jpaProperties\" missing");
		}

		final Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(jpaProperties));
		} catch (Exception e) {
			throw propagate(e);
		}

		final HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher(SHA512.ALGORITHM);
		credentialsMatcher.setHashIterations(1000);
		setCredentialsMatcher(credentialsMatcher);

		this.realmImpl = Guice.createInjector(new ShiroModule() {
			@Override
			protected void configureShiro() {

				install(new JpaPersistModule("ShiroSNAA").properties(properties));
				bind(JPAInitializer.class).asEagerSingleton();

				bind(CredentialsMatcher.class).to(HashedCredentialsMatcher.class);
				bind(HashedCredentialsMatcher.class); // needs to be bound explicitly to have the shiro TypeListener work

				bindConstant().annotatedWith(Names.named("shiro.hashAlgorithmName")).to(SHA512.ALGORITHM);
				bindConstant().annotatedWith(Names.named("shiro.hashIterations")).to(1000);

				bindRealm().to(ShiroSNAAJPARealm.class);
				bind(ShiroSNAAJPARealm.class);

				expose(ShiroSNAAJPARealm.class);

			}
		}
		).getInstance(ShiroSNAAJPARealm.class);
	}

	public String getJpaProperties() {
		return jpaProperties;
	}

	public void setJpaProperties(final String jpaProperties) {
		log.trace("ShiroSNAARealm.setJpaProperties()");
		this.jpaProperties = jpaProperties;
	}
}
