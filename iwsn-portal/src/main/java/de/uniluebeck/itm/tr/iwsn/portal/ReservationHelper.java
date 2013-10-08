package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SecretReservationKeyListRs;
import eu.wisebed.api.v3.common.SecretReservationKey;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.iwsn.common.Base64Helper.decode;
import static de.uniluebeck.itm.tr.iwsn.common.Base64Helper.encode;
import static de.uniluebeck.itm.tr.iwsn.common.json.JSONHelper.fromJSON;
import static de.uniluebeck.itm.tr.iwsn.common.json.JSONHelper.toJSON;
import static java.util.Collections.sort;

public abstract class ReservationHelper {

	private static final Comparator<SecretReservationKey> secretReservationKeyComparator =
			new Comparator<SecretReservationKey>() {
				@Override
				public int compare(final SecretReservationKey o1, final SecretReservationKey o2) {
					return o1.getUrnPrefix().toString().compareTo(o2.getUrnPrefix().toString());
				}
			};


	public static String serialize(final SecretReservationKey secretReservationKey) {
		return serialize(newArrayList(secretReservationKey));
	}

	public static String serialize(final List<SecretReservationKey> secretReservationKeyList) {
		sort(secretReservationKeyList, secretReservationKeyComparator);
		return encode(toJSON(new SecretReservationKeyListRs(secretReservationKeyList)));
	}

	public static String serialize(final Set<SecretReservationKey> secretReservationKeySet) {
		return serialize(newArrayList(secretReservationKeySet));
	}

	public static List<SecretReservationKey> deserialize(final String jsonSerializedSecretReservationKeys) {
		try {
			return fromJSON(decode(jsonSerializedSecretReservationKeys), SecretReservationKeyListRs.class).reservations;
		} catch (Exception e) {
			throw propagate(e);
		}
	}
}
