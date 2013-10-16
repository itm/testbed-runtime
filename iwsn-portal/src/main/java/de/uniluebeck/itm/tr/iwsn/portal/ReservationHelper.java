package de.uniluebeck.itm.tr.iwsn.portal;

import eu.wisebed.api.v3.common.SecretReservationKey;
import org.codehaus.jackson.type.TypeReference;

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
					final int cmp = o1.getUrnPrefix().toString().compareTo(o2.getUrnPrefix().toString());
					if (cmp != 0) {
						return cmp;
					}
					return o1.getKey().compareTo(o2.getKey());
				}
			};


	public static String serialize(final SecretReservationKey secretReservationKey) {
		return serialize(newArrayList(secretReservationKey));
	}

	public static String serialize(final List<SecretReservationKey> secretReservationKeyList) {
		sort(secretReservationKeyList, secretReservationKeyComparator);
		return encode(toJSON(secretReservationKeyList));
	}

	public static String serialize(final Set<SecretReservationKey> secretReservationKeySet) {
		return serialize(newArrayList(secretReservationKeySet));
	}

	public static List<SecretReservationKey> deserialize(final String jsonSerializedSecretReservationKeys) {
		try {
			return fromJSON(
					decode(jsonSerializedSecretReservationKeys),
					new TypeReference<List<SecretReservationKey>>() {}
			);
		} catch (Exception e) {
			throw propagate(e);
		}
	}
}
