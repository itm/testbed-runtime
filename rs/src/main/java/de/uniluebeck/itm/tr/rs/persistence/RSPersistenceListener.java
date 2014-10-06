package de.uniluebeck.itm.tr.rs.persistence;

import eu.wisebed.api.v3.rs.ConfidentialReservationData;

import java.util.List;

public interface RSPersistenceListener {

	void onReservationMade(List<ConfidentialReservationData> crd);

	void onReservationCancelled(List<ConfidentialReservationData> crd);

	void onReservationFinalized(List<ConfidentialReservationData> crd);

}
