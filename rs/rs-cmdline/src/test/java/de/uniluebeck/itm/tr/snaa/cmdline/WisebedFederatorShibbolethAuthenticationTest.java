package de.uniluebeck.itm.tr.snaa.cmdline;

/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.snaa.cmdline.server.SNAAServer;
import de.uniluebeck.itm.tr.snaa.shibboleth.MockShibbolethSNAAModule;
import de.uniluebeck.itm.tr.snaa.wisebed.WisebedSnaaFederator;
import eu.wisebed.api.snaa.*;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class WisebedFederatorShibbolethAuthenticationTest {

	public static final Map<String, String> snaaEndpointPropertiesMapWisebed1 = new HashMap<String, String>() {{
		put("config.port", "8070");
		put("config.snaas", "shib1, shib2, fed1, wisebedfed1");

		put("shib1.type", "shibboleth");
		put("shib1.urnprefix", "urn:wisebed1:shib1");
		put("shib1.path", "/snaa/shib1");
		put("shib1.authorization_class", "eu.wisebed.testbed.api.snaa.authorization.AlwaysAllowAuthorization");

		put("shib2.type", "shibboleth");
		put("shib2.urnprefix", "urn:wisebed1:shib2");
		put("shib2.path", "/snaa/shib2");
		put("shib2.authorization_class", "eu.wisebed.testbed.api.snaa.authorization.AlwaysDenyAuthorization");

		put("fed1.type", "federator");
		put("fed1.path", "/snaa/fed1");
		put("fed1.federates", "shib1, shib2");
		put("fed1.shib1.urnprefixes", "urn:wisebed1:shib1");
		put("fed1.shib1.endpointurl", "http://localhost:8080/snaa/shib1");
		put("fed1.shib2.urnprefixes", "urn:wisebed1:shib2");
		put("fed1.shib2.endpointurl", "http://localhost:8080/snaa/shib2");

		put("wisebedfed1.type", "wisebed-federator");
		put("wisebedfed1.path", "/snaa/wisebedfed1");
		put("wisebedfed1.secret_user_key_url", "https://gridlab23.unibe.ch/portal/SNA/secretUserKey");
		put("wisebedfed1.federates", "shib1");
		put("wisebedfed1.shib1.urnprefixes", "urn:wisebed1:shib1");
		put("wisebedfed1.shib1.endpointurl", "http://localhost:8080/snaa/shib1");
		put("wisebedfed1.shib2.urnprefixes", "urn:wisebed1:shib2");
		put("wisebedfed1.shib2.endpointurl", "http://localhost:8080/snaa/shib2");
	}};

	private Map<String, Set<String>> snaaPrefixSet = null;

	private WisebedSnaaFederator snaaFederator = null;

	private List<eu.wisebed.api.snaa.SecretAuthenticationKey> snaaSecretAuthenticationKeyList =
			new LinkedList<eu.wisebed.api.snaa.SecretAuthenticationKey>();

	private static class SNAAPrefixe {

		private static class Shib1 {

			private static String host = "http://localhost:8070/snaa/shib1";

			private static String urn = "urn:wisebed1:shib1";
		}

		private static class Shib2 {

			private static String host = "http://localhost:8070/snaa/shib2";

			private static String urn = "urn:wisebed1:shib2";
		}
	}

	static {
		// start SNAA endpoint
		try {
			SNAAServer.setMockShibbolethInjector();
			SNAAServer.startFromProperties(getSNAAProperties());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Returns the configuration for the SNAA endpoint.
	 *
	 * @return a {@link java.util.Properties} instance containing the configuration of the dummy SNAA endpoint.
	 */
	private static Properties getSNAAProperties() {
		Properties props = new Properties();
		for (Object key : snaaEndpointPropertiesMapWisebed1.keySet()) {
			props.setProperty((String) key, snaaEndpointPropertiesMapWisebed1.get(key));
		}
		return props;
	}

	@Before
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector(new MockShibbolethSNAAModule());

		Set<String> testbed1 = new HashSet<String>();
		testbed1.add(SNAAPrefixe.Shib1.urn);
		Set<String> testbed2 = new HashSet<String>();
		testbed2.add(SNAAPrefixe.Shib2.urn);

		snaaPrefixSet = new HashMap<String, Set<String>>();
		snaaPrefixSet.put(SNAAPrefixe.Shib1.host, testbed1);
		snaaPrefixSet.put(SNAAPrefixe.Shib2.host, testbed2);
		snaaFederator = new WisebedSnaaFederator(snaaPrefixSet,
				"https://wisebed2.itm.uni-luebeck.de/portal/TARWIS/Welcome/welcomeIndex.php", injector, null
		);

		//creating SNAA-Authentication-Data
		List<AuthenticationTriple> snaaAuthenticationDataShib1 = createAuthenticationData(SNAAPrefixe.Shib1.urn);
		List<AuthenticationTriple> snaaAuthenticationDataShib2 = createAuthenticationData(SNAAPrefixe.Shib2.urn);

		snaaSecretAuthenticationKeyList.addAll(snaaFederator.authenticate(snaaAuthenticationDataShib1));
		snaaSecretAuthenticationKeyList.addAll(snaaFederator.authenticate(snaaAuthenticationDataShib2));
	}

	private LinkedList<AuthenticationTriple> createAuthenticationData(String prefix) {
		AuthenticationTriple snaaAuthenticationTriple = new AuthenticationTriple();
		snaaAuthenticationTriple.setUsername("test@wisebed1.itm.uni-luebeck.de");
		snaaAuthenticationTriple.setPassword("abcdef");
		snaaAuthenticationTriple.setUrnPrefix(prefix);
		LinkedList<AuthenticationTriple> snaaAuthenticationData = new LinkedList<AuthenticationTriple>();
		snaaAuthenticationData.add(snaaAuthenticationTriple);
		return snaaAuthenticationData;
	}

	@Test
	public void checkDecentralizedValidAuthorization()
			throws MalformedURLException, AuthenticationExceptionException, SNAAExceptionException {
		SNAA port = getPort(SNAAPrefixe.Shib1.host);

		Action action = new Action();
		action.setAction("testAction");

		for (String urn : snaaPrefixSet.get(SNAAPrefixe.Shib1.host)) {
			assertTrue(port.isAuthorized(getSecretAuthenticationKeyFromAuthenticationKeyList(urn), action));
		}
	}

	@Test
	public void checkDecentralizedNonValidAuthorization() throws MalformedURLException, SNAAExceptionException {
		SNAA port = getPort(SNAAPrefixe.Shib2.host);

		Action action = new Action();
		action.setAction("testAction");

		for (String urn : snaaPrefixSet.get(SNAAPrefixe.Shib2.host)) {
			assertFalse(port.isAuthorized(getSecretAuthenticationKeyFromAuthenticationKeyList(urn), action));
		}
	}

	private SNAA getPort(String key) throws MalformedURLException {
		URL url = new URL(key);
		SNAAService service = new SNAAService(url);
		return service.getPort(SNAA.class);
	}

	private List<SecretAuthenticationKey> getSecretAuthenticationKeyFromAuthenticationKeyList(String urn) {
		List<SecretAuthenticationKey> keyList = new LinkedList<SecretAuthenticationKey>();
		for (SecretAuthenticationKey key : snaaSecretAuthenticationKeyList) {
			if (key.getUrnPrefix().equals(urn)) {
				keyList.add(key);
			}
		}
		return keyList;
	}
}
