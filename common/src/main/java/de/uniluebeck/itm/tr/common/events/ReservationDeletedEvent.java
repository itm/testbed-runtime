package de.uniluebeck.itm.tr.common.events;

import de.uniluebeck.itm.tr.common.ReservationHelper;
import eu.wisebed.api.v3.common.SecretReservationKey;

import java.util.List;

import static com.google.common.collect.Sets.newHashSet;

public class ReservationDeletedEvent extends ReservationEvent {

	public ReservationDeletedEvent(final List<SecretReservationKey> secretReservationKeys) {
		super(ReservationHelper.serializeSRKs(secretReservationKeys), newHashSet(secretReservationKeys));
	}
}
