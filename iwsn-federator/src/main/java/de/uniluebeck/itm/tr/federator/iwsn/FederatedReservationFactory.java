package de.uniluebeck.itm.tr.federator.iwsn;

import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.wsn.WSN;

import java.util.List;

public interface FederatedReservationFactory {

	FederatedReservation create(final List<ConfidentialReservationData> confidentialReservationDataList,
								final FederatedEndpoints<WSN> wsnFederatedEndpoints);

}
