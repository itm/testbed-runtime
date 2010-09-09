import de.uniluebeck.itm.tr.snaa.cmdline.server.Server;

import java.util.*;

import de.uniluebeck.itm.tr.snaa.federator.FederatorSNAA;
import de.uniluebeck.itm.tr.snaa.wisebed.WisebedSnaaFederator;
import eu.wisebed.testbed.api.snaa.v1.AuthenticationExceptionException;
import eu.wisebed.testbed.api.snaa.v1.AuthenticationTriple;
import eu.wisebed.testbed.api.snaa.v1.SNAAExceptionException;
import eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 27.08.2010
 * Time: 16:58:26
 * To change this template use File | Settings | File Templates.
 */
public class SNAAFederatorTest {
    WisebedSnaaFederator snaaFederator;
    List<SecretAuthenticationKey> authenticatioKeys = new LinkedList<SecretAuthenticationKey>();
    private static final Map<String, String> SNAAPropertiesMapWisebed1 = new HashMap<String, String>() {{

        put("config.port", "8080");
        put("config.snaas", "shib1, fed1, wisebedfed1");

        put("shib1.type", "shibboleth");
        put("shib1.urnprefix", "urn:wisebed1:shib1");
        put("shib1.path", "/snaa/shib1");

        put("fed1.type", "federator");
        put("fed1.path", "/snaa/fed1");
        put("fed1.federates","shib1");
        put("fed1.shib1.urnprefixes", "urn:wisebed1:shib1");
        put("fed1.shib1.endpointurl", "http://localhost:8080/snaa/shib1");

        put("wisebedfed1.type", "wisebed-federator");
        put("wisebedfed1.path", "/snaa/wisebedfed1");
        put("wisebedfed1.secret_user_key_url","http://localhost:8080/snaa/shib1");
        put("wisebedfed1.federates","shib1");
        put("wisebedfed1.shib1.urnprefixes", "urn:wisebed1:shib1");
        put("wisebedfed1.shib1.endpointurl", "http://localhost:8080/snaa/shib1");

    }};

    @Before
    public void setUp() throws Exception {
        Properties SNAAProps1 = new Properties();
        for (String key : SNAAPropertiesMapWisebed1.keySet()) {
            SNAAProps1.setProperty(key, SNAAPropertiesMapWisebed1.get(key));
        }

        List<String> urnPrefixe = new LinkedList<String>();
        urnPrefixe.add("urn:wisebed1:testbed1");

        Server.startFromProperties(SNAAProps1);
        Set<String> testbed1 = new HashSet<String>();
        testbed1.add(urnPrefixe.get(0));

        Map<String, Set<String>> snaaPrefixSet = new HashMap<String, Set<String>>();
        snaaPrefixSet.put("http://localhost:8080/snaa/shib1", testbed1);
        snaaFederator = new WisebedSnaaFederator(snaaPrefixSet, "https://gridlab23.unibe.ch/portal/SNA/secretUserKey");
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
        System.out.println();
    }
}
