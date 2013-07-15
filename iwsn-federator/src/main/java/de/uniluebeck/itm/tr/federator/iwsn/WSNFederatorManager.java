package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.sm.UnknownSecretReservationKeyFault;

import java.util.List;

public interface WSNFederatorManager extends Service {

	WSNFederatorService getWsnFederatorService(final List<SecretReservationKey> secretReservationKeys)
			throws UnknownSecretReservationKeyFault;

	WSNFederatorController getWsnFederatorController(final List<SecretReservationKey> secretReservationKeys);

}
