package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.Set;

public interface ReservationCache extends Service {

    Set<Reservation> getAll();

    Optional<Reservation> lookup(Set<SecretReservationKey> srks);

    Optional<Reservation> lookup(NodeUrn nodeUrn, DateTime timestamp);

    void put(Reservation reservation);

    void clear();

    void remove(Reservation reservation);
}
