package de.uniluebeck.itm.tr.rs.persistence;

import eu.wisebed.api.v3.rs.ConfidentialReservationData;

import java.util.List;

public abstract class OffsetAmountHelper {

	public static List<ConfidentialReservationData> limitResults(final List<ConfidentialReservationData> matchingReservations,
																 Integer offset, Integer amount) {
		if (offset == null) {
			offset = 0;
		}

		if (amount == null) {
			amount = matchingReservations.size();
		}

		//noinspection ConstantConditions
		final int fromIndex = offset;
		final int toIndex = matchingReservations.size() < (fromIndex + amount) ?
				(matchingReservations.size()) :
				(fromIndex + amount);

		return matchingReservations.subList(fromIndex, toIndex);
	}

}
