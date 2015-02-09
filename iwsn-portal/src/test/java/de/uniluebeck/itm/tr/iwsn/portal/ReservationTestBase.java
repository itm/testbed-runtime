package de.uniluebeck.itm.tr.iwsn.portal;

import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.Interval;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.joda.time.DateTime.now;

public class ReservationTestBase {

    protected static final String USERNAME = "My Awesome Username";

    protected static final NodeUrnPrefix NODE_URN_PREFIX = new NodeUrnPrefix("urn:unit-test:");

    protected static final NodeUrn RESERVATION_1_NODE_URN = new NodeUrn(NODE_URN_PREFIX + "0x0001");
    protected static final NodeUrn RESERVATION_2_NODE_URN = new NodeUrn(NODE_URN_PREFIX + "0x0002");
    protected static final NodeUrn RESERVATION_3_NODE_URN = new NodeUrn(NODE_URN_PREFIX + "0x0003");

    protected static final Set<NodeUrn> RESERVATION_1_NODE_URN_SET = newHashSet(RESERVATION_1_NODE_URN);
    protected static final Set<NodeUrn> RESERVATION_2_NODE_URN_SET = newHashSet(RESERVATION_2_NODE_URN);
    protected static final Set<NodeUrn> RESERVATION_3_NODE_URN_SET = newHashSet(RESERVATION_3_NODE_URN);

    protected static final NodeUrn UNKNOWN_NODE_URN = new NodeUrn(NODE_URN_PREFIX + "0x9999");

    protected static final SecretReservationKey RESERVATION_1_SRK;
    protected static final SecretReservationKey RESERVATION_2_SRK;
    protected static final SecretReservationKey RESERVATION_3_SRK;
    protected static final SecretReservationKey UNKNOWN_SRK;

    static {
        RESERVATION_1_SRK = new SecretReservationKey().withKey("YOU_KNOWN_ME_ONE").withUrnPrefix(NODE_URN_PREFIX);
        RESERVATION_2_SRK = new SecretReservationKey() .withKey("YOU_KNOWN_ME_TWO").withUrnPrefix(NODE_URN_PREFIX);
        RESERVATION_3_SRK = new SecretReservationKey().withKey("YOU_KNOWN_ME_THREE").withUrnPrefix(NODE_URN_PREFIX);
        UNKNOWN_SRK = new SecretReservationKey().withKey("YOU_DO_NOT_KNOW_ME").withUrnPrefix(NODE_URN_PREFIX);
    }

    protected static final Interval RESERVATION_1_INTERVAL = new Interval(now(), now().plusHours(1));
    protected static final Interval RESERVATION_2_INTERVAL = new Interval(now().plusMinutes(1), now().plusHours(1));
    protected static final Interval RESERVATION_3_INTERVAL = new Interval(now().minusHours(1), now().minusMinutes(1));

    protected static final Set<SecretReservationKey> RESERVATION_1_SRK_SET = newHashSet(RESERVATION_1_SRK);
    protected static final Set<SecretReservationKey> RESERVATION_2_SRK_SET = newHashSet(RESERVATION_2_SRK);
    protected static final Set<SecretReservationKey> RESERVATION_3_SRK_SET = newHashSet(RESERVATION_3_SRK);
    protected static final Set<SecretReservationKey> UNKNOWN_SRK_SET = newHashSet(UNKNOWN_SRK);

    protected static final ConfidentialReservationData RESERVATION_1_DATA = new ConfidentialReservationData()
            .withFrom(RESERVATION_1_INTERVAL.getStart())
            .withTo(RESERVATION_1_INTERVAL.getEnd())
            .withNodeUrns(RESERVATION_1_NODE_URN_SET)
            .withUsername(USERNAME)
            .withSecretReservationKey(RESERVATION_1_SRK);

    protected static final ConfidentialReservationData RESERVATION_2_DATA = new ConfidentialReservationData()
            .withFrom(RESERVATION_2_INTERVAL.getStart())
            .withTo(RESERVATION_2_INTERVAL.getEnd())
            .withNodeUrns(RESERVATION_2_NODE_URN_SET)
            .withUsername(USERNAME)
            .withSecretReservationKey(RESERVATION_2_SRK);

    protected static final ConfidentialReservationData RESERVATION_3_DATA = new ConfidentialReservationData()
            .withFrom(RESERVATION_3_INTERVAL.getStart())
            .withTo(RESERVATION_3_INTERVAL.getEnd())
            .withNodeUrns(RESERVATION_3_NODE_URN_SET)
            .withUsername(USERNAME)
            .withSecretReservationKey(RESERVATION_3_SRK);

    protected static final Set<ConfidentialReservationData> RESERVATION_1_DATA_SET = newHashSet(RESERVATION_1_DATA);
    protected static final Set<ConfidentialReservationData> RESERVATION_2_DATA_SET = newHashSet(RESERVATION_2_DATA);
    protected static final Set<ConfidentialReservationData> RESERVATION_3_DATA_SET = newHashSet(RESERVATION_3_DATA);
}
