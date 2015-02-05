package de.uniluebeck.itm.tr.iwsn.portal;

import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class ReservationTestBase {

    protected static final String USERNAME = "My Awesome Username";

    protected static final NodeUrnPrefix NODE_URN_PREFIX = new NodeUrnPrefix("urn:unit-test:");

    protected static final NodeUrn RESERVATION_1_NODE_URN = new NodeUrn(NODE_URN_PREFIX + "0x0001");

    protected static final Set<NodeUrn> RESERVATION_NODE_URNS_1 = newHashSet(RESERVATION_1_NODE_URN);

    protected static final NodeUrn RESERVATION_2_NODE_URN = new NodeUrn(NODE_URN_PREFIX + "0x0002");

    protected static final Set<NodeUrn> RESERVATION_NODE_URNS_2 = newHashSet(RESERVATION_2_NODE_URN);

    protected static final NodeUrn RESERVATION_3_NODE_URN = new NodeUrn(NODE_URN_PREFIX + "0x0003");

    protected static final Set<NodeUrn> RESERVATION_NODE_URNS_3 = newHashSet(RESERVATION_3_NODE_URN);


    static {
        KNOWN_SECRET_RESERVATION_KEY_1 = new SecretReservationKey()
                .withKey("YOU_KNOWN_ME_ONE")
                .withUrnPrefix(NODE_URN_PREFIX);

        KNOWN_SECRET_RESERVATION_KEY_2 = new SecretReservationKey()
                .withKey("YOU_KNOWN_ME_TWO")
                .withUrnPrefix(NODE_URN_PREFIX);

        KNOWN_SECRET_RESERVATION_KEY_3 = new SecretReservationKey()
                .withKey("YOU_KNOWN_ME_THREE")
                .withUrnPrefix(NODE_URN_PREFIX);

        UNKNOWN_SECRET_RESERVATION_KEY_1 = new SecretReservationKey()
                .withKey("YOU_KNOWN_ME_THREE")
                .withUrnPrefix(NODE_URN_PREFIX);
    }

    protected static final Interval RESERVATION_INTERVAL_1 = new Interval(
            DateTime.now(),
            DateTime.now().plusHours(1)
    );

    protected static final Interval RESERVATION_INTERVAL_2 = new Interval(
            DateTime.now().plusMinutes(1),
            DateTime.now().plusHours(1)
    );

    protected static final Interval RESERVATION_INTERVAL_3 = new Interval(
            DateTime.now().minusHours(1),
            DateTime.now().minusMinutes(1)
    );


    protected static final SecretReservationKey KNOWN_SECRET_RESERVATION_KEY_1;

    protected static final Set<SecretReservationKey> KNOWN_SECRET_RESERVATION_KEY_SET_1 =
            newHashSet(KNOWN_SECRET_RESERVATION_KEY_1);

    protected static final ConfidentialReservationData RESERVATION_DATA_1 = new ConfidentialReservationData()
            .withFrom(RESERVATION_INTERVAL_1.getStart())
            .withTo(RESERVATION_INTERVAL_1.getEnd())
            .withNodeUrns(RESERVATION_NODE_URNS_1)
            .withUsername(USERNAME)
            .withSecretReservationKey(KNOWN_SECRET_RESERVATION_KEY_1);

    protected static final SecretReservationKey KNOWN_SECRET_RESERVATION_KEY_2;

    protected static final Set<SecretReservationKey> KNOWN_SECRET_RESERVATION_KEY_SET_2 =
            newHashSet(KNOWN_SECRET_RESERVATION_KEY_2);

    protected static final ConfidentialReservationData RESERVATION_DATA_2 = new ConfidentialReservationData()
            .withFrom(RESERVATION_INTERVAL_2.getStart())
            .withTo(RESERVATION_INTERVAL_2.getEnd())
            .withNodeUrns(RESERVATION_NODE_URNS_2)
            .withUsername(USERNAME)
            .withSecretReservationKey(KNOWN_SECRET_RESERVATION_KEY_2);

    protected static final SecretReservationKey KNOWN_SECRET_RESERVATION_KEY_3;

    protected static final Set<SecretReservationKey> KNOWN_SECRET_RESERVATION_KEY_SET_3 =
            newHashSet(KNOWN_SECRET_RESERVATION_KEY_3);

    protected static final ConfidentialReservationData RESERVATION_DATA_3 = new ConfidentialReservationData()
            .withFrom(RESERVATION_INTERVAL_3.getStart())
            .withTo(RESERVATION_INTERVAL_3.getEnd())
            .withNodeUrns(RESERVATION_NODE_URNS_3)
            .withUsername(USERNAME)
            .withSecretReservationKey(KNOWN_SECRET_RESERVATION_KEY_3);

    protected static final SecretReservationKey UNKNOWN_SECRET_RESERVATION_KEY_1;

    protected static final Set<SecretReservationKey> UNKNOWN_SECRET_RESERVATION_KEY_SET =
            newHashSet(UNKNOWN_SECRET_RESERVATION_KEY_1);

}
