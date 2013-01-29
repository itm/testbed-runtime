package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerImpl;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;

public class DeliveryManagerAdapter extends DeliveryManagerImpl {

	private final Reservation reservation;

	@Inject
	public DeliveryManagerAdapter(@Assisted final Reservation reservation) {
		this.reservation = reservation;
	}

	@Override
	protected void doStart() {
		super.doStart();    // TODO implement
	}

	@Override
	protected void doStop() {
		super.doStop();    // TODO implement
	}
}
