package de.uniluebeck.itm.tr.common;

import eu.wisebed.api.v3.common.SecretReservationKey;

import java.util.List;

public interface SessionManagementPreconditions {

	void checkGetInstanceArguments(List<SecretReservationKey> secretReservationKey);

	void checkGetInstanceArguments(List<SecretReservationKey> secretReservationKey,
								   boolean singleUrnImplementation);
}
