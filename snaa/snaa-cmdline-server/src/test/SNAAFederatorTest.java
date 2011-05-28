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

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.snaa.cmdline.server.SNAAServer;

import java.util.*;

import de.uniluebeck.itm.tr.snaa.federator.FederatorSNAA;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAModule;
import de.uniluebeck.itm.tr.snaa.wisebed.WisebedSnaaFederator;
import eu.wisebed.api.snaa.AuthenticationExceptionException;
import eu.wisebed.api.snaa.AuthenticationTriple;
import eu.wisebed.api.snaa.SNAAExceptionException;
import eu.wisebed.api.snaa.SecretAuthenticationKey;
import org.junit.Before;
import org.junit.Test;

public class SNAAFederatorTest {
    WisebedSnaaFederator snaaFederator;
    List<SecretAuthenticationKey> authenticatioKeys = new LinkedList<SecretAuthenticationKey>();
    private static final Map<String, String> SNAAPropertiesMapWisebed1 = new HashMap<String, String>() {{

        put("config.port", "8080");
        put("config.snaas", "shib1, fed1, wisebedfed1");

        put("shib1.type", "shibboleth");
        put("shib1.urnprefix", "urn:wisebed1:shib1");
        put("shib1.path", "/snaa/shib1");
        put("shib1.authorization.url","https://wisebed2.itm.uni-luebeck.de/portal/TARWIS/Welcome/welcomeIndex.php");

        put("fed1.type", "federator");
        put("fed1.path", "/snaa/fed1");
        put("fed1.federates","shib1");
        put("fed1.shib1.urnprefixes", "urn:wisebed1:shib1");
        put("fed1.shib1.endpointurl", "http://localhost:8080/snaa/shib1");

        put("wisebedfed1.type", "wisebed-federator");
        put("wisebedfed1.path", "/snaa/wisebedfed1");
        put("wisebedfed1.authentication.url","https://wisebed2.itm.uni-luebeck.de/portal/TARWIS/Welcome/welcomeIndex.php");
        put("wisebedfed1.federates","shib1");
        put("wisebedfed1.shib1.urnprefixes", "urn:wisebed1:shib1");
        put("wisebedfed1.shib1.endpointurl", "http://localhost:8080/snaa/shib1");

    }};

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new ShibbolethSNAAModule());
        Properties SNAAProps1 = new Properties();
        for (String key : SNAAPropertiesMapWisebed1.keySet()) {
            SNAAProps1.setProperty(key, SNAAPropertiesMapWisebed1.get(key));
        }

        List<String> urnPrefixe = new LinkedList<String>();
        urnPrefixe.add("urn:wisebed1:testbed1");

        SNAAServer.startFromProperties(SNAAProps1);
        Set<String> testbed1 = new HashSet<String>();
        testbed1.add(urnPrefixe.get(0));

        Map<String, Set<String>> snaaPrefixSet = new HashMap<String, Set<String>>();
        snaaPrefixSet.put("http://localhost:8080/snaa/shib1", testbed1);
        snaaFederator = new WisebedSnaaFederator(snaaPrefixSet, "https://wisebed2.itm.uni-luebeck.de/portal/TARWIS/Welcome/welcomeIndex.php", injector, null);
    }

    @Test
    public void test() throws AuthenticationExceptionException, SNAAExceptionException {
        AuthenticationTriple triple = new AuthenticationTriple();
        triple.setUsername("rohwedder@wisebed1.itm.uni-luebeck.de");
        triple.setPassword("Hztsmfaqt");
        triple.setUrnPrefix("urn:wisebed1:testbed1");
        List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
        authenticationData.add(triple);
        authenticatioKeys = snaaFederator.authenticate(authenticationData);
    }
}
