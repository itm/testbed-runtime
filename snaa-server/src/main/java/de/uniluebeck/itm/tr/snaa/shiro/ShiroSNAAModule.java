package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAProperties;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.subject.Subject;

import javax.persistence.EntityManager;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Extension of the abstract {@link ShiroModule} to configure Apache Shiro and to bind dependencies
 * for {@link ShiroSNAA}.
 */
public class ShiroSNAAModule extends ShiroModule {

	private static final String DEFAULT_HASH_ALGORITHM_NAME = Sha512Hash.ALGORITHM_NAME;

	private static final int DEFAULT_HASH_ITERATIONS = 1000;

	/**
	 * The name of the hash algorithm to be used for hashing a user's password .
	 */
	private final String hashAlgorithmName;

	/**
	 * The number of hash hashIterations to be applied on a user's password.
	 */
	private final int hashIterations;

	private final Set<NodeUrnPrefix> servedNodeUrnPrefixes;

	private final String snaaContextPath;

	/**
	 * Constructor setting up the default credential matcher which hashes a provided password 1000
	 * times using sha512 hash algorithm.
	 */
	public ShiroSNAAModule(final SNAAConfig config) {
		this.hashAlgorithmName = DEFAULT_HASH_ALGORITHM_NAME;
		this.hashIterations = DEFAULT_HASH_ITERATIONS;
		this.servedNodeUrnPrefixes = newHashSet(config.getUrnPrefix());
		this.snaaContextPath = config.getSnaaContextPath();
	}

	public ShiroSNAAModule(final String snaaContextPath, final Set<NodeUrnPrefix> servedNodeUrnPrefixes) {
		this(snaaContextPath, DEFAULT_HASH_ALGORITHM_NAME, DEFAULT_HASH_ITERATIONS, servedNodeUrnPrefixes);
	}

	public ShiroSNAAModule(final String snaaContextPath,
						   final String hashAlgorithmName,
						   final int hashIterations,
						   final Set<NodeUrnPrefix> servedNodeUrnPrefixes) {
		this.snaaContextPath = checkNotNull(snaaContextPath);
		this.hashAlgorithmName = checkNotNull(hashAlgorithmName);
		this.hashIterations = checkNotNull(hashIterations);
		this.servedNodeUrnPrefixes = checkNotNull(servedNodeUrnPrefixes);
	}

	@Override
	protected void configureShiro() {
		try {

			requireBinding(EntityManager.class);
			requireBinding(ServicePublisher.class);

			bind(String.class).annotatedWith(Names.named(SNAAProperties.CONTEXT_PATH)).toInstance(snaaContextPath);
			bind(ServedNodeUrnPrefixesProvider.class).toInstance(new ServedNodeUrnPrefixesProvider() {
				@Override
				public Set<NodeUrnPrefix> get() {
					return servedNodeUrnPrefixes;
				}
			}
			);

			bind(CredentialsMatcher.class).to(HashedCredentialsMatcher.class);
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

	/**
	 * Provides a credentials matcher which hashes a user's password a configurable number of times.
	 *
	 * @return A credentials matcher which hashes a user's password a configurable number of times.
	 */
	@Provides
	private HashedCredentialsMatcher provideHashedCredentialsMatcher() {
		HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher(hashAlgorithmName);
		hashedCredentialsMatcher.setHashIterations(hashIterations);
		return hashedCredentialsMatcher;
	}

	@Provides
	private Subject provideCurrentUser() {
		return SecurityUtils.getSubject();
	}
}