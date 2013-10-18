package de.uniluebeck.itm.tr.rs.persistence;

import eu.wisebed.api.v3.rs.ConfidentialReservationData;

import java.util.Comparator;

public class ConfidentialReservationDataComparator implements Comparator<ConfidentialReservationData> {

	@Override
	public int compare(final ConfidentialReservationData o1, final ConfidentialReservationData o2) {

		// sort descending by start date and if start date is equal, additionally sort by end date
		if (o1.getFrom().equals(o2.getFrom())) {
			final int i = o1.getTo().compareTo(o2.getTo());
			return (i == 0 ? -1 : i) * -1;
		}

		final int i = o1.getFrom().compareTo(o2.getFrom());
		return (i == 0 ? -1 : i) * -1;
	}
}
