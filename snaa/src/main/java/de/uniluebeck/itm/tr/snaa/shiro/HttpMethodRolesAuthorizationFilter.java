package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.common.collect.Lists;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.RolesAuthorizationFilter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.addAll;

public class HttpMethodRolesAuthorizationFilter extends RolesAuthorizationFilter {

	private static enum HttpMethod {

		GET, DELETE, HEAD, MKCOL, OPTIONS, POST, PUT, TRACE;

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}

		public static HttpMethod parse(String method) {
			for (HttpMethod httpMethod : values()) {
				if (httpMethod.toString().equals(method.toLowerCase())) {
					return httpMethod;
				}
			}
			throw new IllegalArgumentException("Unknown HTTP method: " + method);
		}
	}

	@Override
	public boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws
			IOException {

		Subject subject = getSubject(request, response);
		String[] rolesArray = (String[]) mappedValue;

		if (rolesArray == null || rolesArray.length == 0) {
			//no roles specified, so nothing to check - allow access.
			return true;
		}

		final Map<HttpMethod, Set<String>> methodToRolesMapping = new HashMap<HttpMethod, Set<String>>();
		for (String rolesEntry : rolesArray) {

			final String[] kv = rolesEntry.split("=");
			final HttpMethod httpMethod = HttpMethod.parse(kv[0]);
			final String[] roles = kv[1].split("|");
			final HashSet<String> rolesSet = new HashSet<String>(roles.length);
			addAll(rolesSet, roles);

			methodToRolesMapping.put(httpMethod, rolesSet);
		}

		final HttpMethod requestMethod = HttpMethod.parse(((HttpServletRequest) request).getMethod());
		final Set<String> roleIdentifiers = methodToRolesMapping.get(requestMethod);

		if (roleIdentifiers == null) {
			return true;
		}

		final boolean[] hasRoles = subject.hasRoles(Lists.newArrayList(roleIdentifiers));
		for (boolean hasRole : hasRoles) {
			if (hasRole) {
				return true;
			}
		}

		return false;
	}
}
