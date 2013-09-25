package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.snaa.shiro.dao.UserDao;
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

import java.util.HashSet;
import java.util.Set;

/**
 * JPA based authorization realm used by Apache Shiro
 */
public class ShiroSNAAJPARealm extends AuthorizingRealm {

	/**
	 * Object use to access persisted user information
	 */
	@Inject
	private UserDao usersDao;

	// ------------------------------------------------------------------------
	/**
	 * Constructor installing the provided credentials matcher which supports hashing of provided
	 * credentials.
	 * 
	 * @param hashedCredentialsMatcher
	 *            The credentials matcher to be used
	 */
	@Inject
	public ShiroSNAAJPARealm(final HashedCredentialsMatcher hashedCredentialsMatcher) {
		setCredentialsMatcher(hashedCredentialsMatcher);
	}

	@Override
	protected SimpleAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
		UsernamePasswordToken token = (UsernamePasswordToken) authToken;
		User user = usersDao.find(token.getUsername());
		if (user != null && user.getSalt() != null && !user.getSalt().equals("")) {
			return new SimpleAuthenticationInfo(user.getName(), user.getPassword(), new SimpleByteSource(user.getSalt()), getName());
		} else if (user != null) {
			return new SimpleAuthenticationInfo(user.getName(), user.getPassword(), getName());
		}

		return null;

	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String userId = (String) principals.fromRealm(getName()).iterator().next();
		User user = usersDao.find(userId);
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

	public boolean doesUserExist(final String userId) {
		return usersDao.find(userId) != null;
	}
}
