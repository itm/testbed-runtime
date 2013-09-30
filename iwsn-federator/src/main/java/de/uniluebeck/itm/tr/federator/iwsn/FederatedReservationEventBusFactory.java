package de.uniluebeck.itm.tr.federator.iwsn;

public interface FederatedReservationEventBusFactory {

	FederatedReservationEventBus create(WSNFederatorService wsnFederatorService,
										WSNFederatorController wsnFederatorController);

}
