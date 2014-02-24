package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Permission;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.SimpleByteSource;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA based authorization realm used by Apache Shiro
 */
public class ShiroSNAAJPARealm extends AuthorizingRealm {

	private final Provider<EntityManager> emProvider;

	// ------------------------------------------------------------------------
	/**
	 * Constructor installing the provided credentials matcher which supports hashing of provided
	 * credentials.
	 * 
	 * @param hashedCredentialsMatcher
	 *            The credentials matcher to be used
	 */
	@Inject
	public ShiroSNAAJPARealm(final HashedCredentialsMatcher hashedCredentialsMatcher,
							 final Provider<EntityManager> emProvider) {
		this.emProvider = emProvider;
		setCredentialsMatcher(hashedCredentialsMatcher);
	}

	@Override
	@Transactional
	protected SimpleAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
		UsernamePasswordToken token = (UsernamePasswordToken) authToken;
		final User user = emProvider.get().find(User.class, token.getUsername());
		if (user != null && user.getSalt() != null && !user.getSalt().equals("")) {
			return new SimpleAuthenticationInfo(user.getEmail(), user.getPassword(), new SimpleByteSource(user.getSalt()), getName());
		} else if (user != null) {
			return new SimpleAuthenticationInfo(user.getEmail(), user.getPassword(), getName());
		}

		return null;

	}

	@Override
	@Transactional
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String username = (String) principals.fromRealm(getName()).iterator().next();
		final User user = emProvider.get().find(User.class, username);
		if (user != null) {
			SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
			for (Role role : user.getRoles()) {
				info.addRole(role.getName());
				Set<Permission> permissions = role.getPermissions();
				Set<String> strPerm = toString(permissions);
				info.addStringPermissions(strPerm);
			}
			return info;
		}

		return null;
	}

	// ------------------------------------------------------------------------
	/**
	 * Converts a set of {@link Permission} objects into a set of Strings and returns the result.
	 * 
	 * @param permissions
	 *            A set of persisted permission objects which indicate which action is allowed for
	 *            which resource groups
	 * @return A set of permission stings which indicate which action is allowed for which resource
	 *         groups
	 */
	private Set<String> toString(final Set<Permission> permissions) {
		Set<String> result = new HashSet<String>();
		for (Permission permission : permissions) {
			result.add(permission.getAction().getName() + ":" + permission.getResourceGroup().getName());
		}
		return result;
	}

	@Transactional
	public boolean doesUserExist(final String username) {
		return emProvider.get().find(User.class, username) != null;
	}
}
