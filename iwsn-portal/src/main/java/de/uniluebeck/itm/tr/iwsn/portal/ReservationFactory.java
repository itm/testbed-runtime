package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.assistedinject.Assisted;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.Interval;

import java.util.List;
import java.util.Set;

public interface ReservationFactory {

	Reservation create(
			List<ConfidentialReservationData> confidentialReservationDataList,
			@Assisted("secretReservationKey") String key,
			@Assisted("username") String username,
			Set<NodeUrn> nodeUrns,
			Interval interval
	);

}
