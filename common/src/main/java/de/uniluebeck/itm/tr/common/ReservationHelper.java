package de.uniluebeck.itm.tr.common;

import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.codehaus.jackson.type.TypeReference;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.Base64Helper.decode;
import static de.uniluebeck.itm.tr.common.Base64Helper.encode;
import static de.uniluebeck.itm.tr.common.json.JSONHelper.fromJSON;
import static de.uniluebeck.itm.tr.common.json.JSONHelper.toJSON;
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
		return serializeSRKs(newArrayList(secretReservationKey));
	}

	public static String serialize(final Set<SecretReservationKey> secretReservationKeySet) {
		return serializeSRKs(newArrayList(secretReservationKeySet));
	}

	public static String serialize(final List<ConfidentialReservationData> crds) {
		final List<SecretReservationKey> srks = newArrayList();
		for (ConfidentialReservationData crd : crds) {
			srks.add(crd.getSecretReservationKey());
		}
		return serializeSRKs(srks);
	}

	public static Set<SecretReservationKey> deserialize(final String secretReservationKeysBase64) {
		try {
			final List<SecretReservationKey> keys = fromJSON(
					decode(secretReservationKeysBase64),
					new TypeReference<List<SecretReservationKey>>() {
					}
			);
			return newHashSet(keys);
		} catch (Exception e) {
			throw propagate(e);
		}
	}

	public static List<SecretReservationKey> deserializeToList(final String secretReservationKeysBase64) {
		final List<SecretReservationKey> list = newArrayList(deserialize(secretReservationKeysBase64));
		sort(list, secretReservationKeyComparator);
		return list;
	}

	public static String serializeSRKs(final List<SecretReservationKey> secretReservationKeyList) {
		sort(secretReservationKeyList, secretReservationKeyComparator);
		return encode(toJSON(secretReservationKeyList));
	}

	public static boolean equalsDeserialized(final String one, final String another) {
		return deserialize(one).equals(deserialize(another));
	}
}
