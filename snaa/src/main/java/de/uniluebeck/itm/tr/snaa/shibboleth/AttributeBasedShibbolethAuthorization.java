/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.Inject;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AttributeBasedShibbolethAuthorization implements ShibbolethAuthorization {

	private static final Logger log = LoggerFactory.getLogger(AttributeBasedShibbolethAuthorization.class);

	private final AttributeBasedShibbolethAuthorizationAttributes attributes;

	private final AttributeBasedShibbolethAuthorizationDataSource dataSource;

	@Inject
	public AttributeBasedShibbolethAuthorization(
			final AttributeBasedShibbolethAuthorizationAttributes attributes,
			final AttributeBasedShibbolethAuthorizationDataSource dataSource) {
		this.attributes = attributes;
		this.dataSource = dataSource;
	}

	@Override
	public boolean isAuthorized(final Action action, final String username, final Map<String, List<Object>> userDetails)
			throws SNAAFault_Exception {

		String personUniqueId;
		//check if user is authorised in datasource
		try {
			//get uid

			List<Object> uidList = userDetails.get("personUniqueID");
			if (uidList == null) {
				return false;
			}

			personUniqueId = (String) uidList.get(0);

			//check authorization for attribute-Map
			for (String key : userDetails.keySet()) {
				String regex = getRegex(key);
				if (regex != null) {
					if (!compareValues(regex, userDetails.get(key))) {
						throw new Exception();
					}
				}
			}

			//check datasource
			return dataSource.isAuthorized(personUniqueId, action.toString());
		} catch (Exception e) {
			log.warn(e.getMessage());
			return false;
		}
	}


	private String getRegex(Object key) {
		if (attributes == null) {
			return null;
		}
		for (Object keyRegex : attributes.keySet()) {
			String keyRegexString = (String) keyRegex;
			if (((String) key).matches(keyRegexString)) {
				return keyRegexString;
			}
		}
		return null;
	}

	private boolean compareValues(String regex, List<Object> cmpValues) {
		for (Object value : cmpValues) {
			if (!compareValue(regex, value)) {
				log.warn("no matching of: " + regex + " on " + value);
				return false;
			}
		}
		return true;
	}

	private boolean compareValue(String regex, Object value) {
		return (((String) value).matches(attributes.get(regex)));
	}

}