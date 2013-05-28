package de.uniluebeck.itm.tr.snaa.shiro;

import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.guice.ShiroModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Extension of the abstract {@link ShiroModule} to configure Apache Shiro and to bind dependencies
 * for {@link ShiroSNAA}
 */
public class ShiroSNAAModule extends ShiroModule {

	/** The name of the hash algorithm to be used for hashing a user's password . */
	private String hashAlgorithmName;

	/** The number of hash iterations to be applied on a user's password. */
	private int iterations;

	/**
	 * Constructor setting up the default credential matcher which hashes a provided password 1000
	 * times using sha512 hash algorithm.
	 */
	public ShiroSNAAModule() {
		hashAlgorithmName = Sha512Hash.ALGORITHM_NAME;
		iterations = 1000;
	}

	/**
	 * Constructor
	 * 
	 * @param hashAlgorithm
	 *            The hash algorithm to be used for hashing a user's password
	 */
	public ShiroSNAAModule(SimpleHash hashAlgorithm) {
		hashAlgorithmName = hashAlgorithm.getAlgorithmName();
		this.iterations = hashAlgorithm.getIterations();
	}

	@Override
	protected void configureShiro() {

		try {
			bind(CredentialsMatcher.class).to(HashedCredentialsMatcher.class);
			bindRealm().to(TRJPARealm.class).in(Singleton.class);
			install(new FactoryModuleBuilder().build(ShiroSNAAFactory.class));
			expose(ShiroSNAAFactory.class);

		} catch (Exception e) {
			addError(e);
		}
	}

	/**
	 * Provides a credentials matcher which hashes a user's password a configurable number of times.
	 * @return A credentials matcher which hashes a user's password a configurable number of times.
	 */
	@Provides
	private HashedCredentialsMatcher provideHashedCredentialsMatcher() {
		HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher(hashAlgorithmName);
		hashedCredentialsMatcher.setHashIterations(iterations);
		return hashedCredentialsMatcher;
	}

}