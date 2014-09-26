package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.assistedinject.Assisted;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.List;
import java.util.Set;

public interface ReservationFactory {

	Reservation create(
			List<ConfidentialReservationData> confidentialReservationDataList,
			@Assisted("secretReservationKey") String key,
			@Assisted("username") String username,
            @Assisted("cancelled") DateTime cancelled,
            @Assisted("finalized") DateTime finalized,
			Set<NodeUrn> nodeUrns,
			Interval interval
	);

}
