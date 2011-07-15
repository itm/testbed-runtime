package de.uniluebeck.itm.wisebed.cmdlineclient.wrapper;

import de.uniluebeck.itm.wisebed.cmdlineclient.BeanShellHelper;
import eu.wisebed.api.sm.SecretReservationKey;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class BeanShellHelperTest {

	@Test
	public void testParseSecretReservationKeys() throws Exception {

		String testString = "urn:wisebed:uzl-staging:,E4017A2318D9F3D8F96175F1C59DCAE0;urn:wisebed:uzl1:,A2F6828E19AA7C2E6D5E572A8A01E495";
		final List<SecretReservationKey> secretReservationKeyList =
				BeanShellHelper.parseSecretReservationKeys(testString);

		assertEquals(2, secretReservationKeyList.size());
		assertEquals("urn:wisebed:uzl-staging:", secretReservationKeyList.get(0).getUrnPrefix());
		assertEquals("E4017A2318D9F3D8F96175F1C59DCAE0", secretReservationKeyList.get(0).getSecretReservationKey());
		assertEquals("urn:wisebed:uzl1:", secretReservationKeyList.get(1).getUrnPrefix());
		assertEquals("A2F6828E19AA7C2E6D5E572A8A01E495", secretReservationKeyList.get(1).getSecretReservationKey());
	}
}
