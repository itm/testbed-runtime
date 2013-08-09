/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.jaas.htpasswd;

import java.security.Principal;

/**
 * Basic Principal implementation.
 */
public class HtpasswdPrincipal implements Principal {

	/**
	 * The principal's name.
	 */
	private String principalName;

	/**
	 * Constructor.
	 *
	 * @param name the principal name
	 */
	public HtpasswdPrincipal(String name) {
		principalName = name;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return principalName;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return principalName.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof HtpasswdPrincipal) {
			return ((HtpasswdPrincipal) obj).getName().equals(principalName);
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return new StringBuffer("HtpasswdPrincipal[").append(principalName).append("]").toString();
	}
}