package de.uniluebeck.itm.tr.rs.federator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import eu.wisebed.api.rs.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

abstract class FederatorRSHelper {

	static Map<RS, List<SecretAuthenticationKey>> constructEndpointToAuthenticationMap(
			final FederationManager<RS> federationManager,
			final List<SecretAuthenticationKey> secretAuthenticationKey) throws RSExceptionException {

		Map<RS, List<SecretAuthenticationKey>> map = newHashMap();

		for (SecretAuthenticationKey authenticationKey : secretAuthenticationKey) {

			RS rs = federationManager.getEndpointByUrnPrefix(authenticationKey.getUrnPrefix());

			if (rs == null) {
				String msg = "The node URN prefix " +
						authenticationKey.getUrnPrefix() +
						" is not served by this RS instance!";

				RSException exception = new RSException();
				exception.setMessage(msg);
				throw new RSExceptionException(msg, exception);
			}

			List<SecretAuthenticationKey> secretReservationKeyList = map.get(rs);
			if (secretReservationKeyList == null) {
				secretReservationKeyList = new LinkedList<SecretAuthenticationKey>();
				map.put(rs, secretReservationKeyList);
			}
			secretReservationKeyList.add(authenticationKey);
		}
		return map;
	}

	static Map<RS, List<SecretReservationKey>> constructEndpointToReservationKeyMap(
			final FederationManager<RS> federationManager,
			final List<SecretReservationKey> secretReservationKey) throws RSExceptionException {

		Map<RS, List<SecretReservationKey>> map = newHashMap();

		for (SecretReservationKey reservationKey : secretReservationKey) {

			RS rs = federationManager.getEndpointByUrnPrefix(reservationKey.getUrnPrefix());

			if (rs == null) {
				String msg = "The node URN prefix "
						+ reservationKey.getUrnPrefix() +
						" is not served by this RS instance!";

				RSException exception = new RSException();
				exception.setMessage(msg);
				throw new RSExceptionException(msg, exception);
			}

			List<SecretReservationKey> secretReservationKeyList = map.get(rs);
			if (secretReservationKeyList == null) {
				secretReservationKeyList = new LinkedList<SecretReservationKey>();
				map.put(rs, secretReservationKeyList);
			}
			secretReservationKeyList.add(reservationKey);

		}
		return map;
	}

	static BiMap<RS, ConfidentialReservationData> constructEndpointToReservationMap(
			final FederationManager<RS> federationManager,
			final ConfidentialReservationData reservation) {

		BiMap<RS, ConfidentialReservationData> map = HashBiMap.create(federationManager.getEndpoints().size());

		for (String nodeURN : reservation.getNodeURNs()) {

			RS rs = federationManager.getEndpointByNodeUrn(nodeURN);

			ConfidentialReservationData data = map.get(rs);
			if (data == null) {
				data = new ConfidentialReservationData();
				map.put(rs, data);
			}

			data.getNodeURNs().add(nodeURN);
			data.setFrom(reservation.getFrom());
			data.setTo(reservation.getTo());
			data.setUserData(reservation.getUserData());
			data.getData().addAll(reservation.getData());
		}

		return map;

	}

	static BiMap<RS, List<SecretAuthenticationKey>> constructEndpointToAuthenticationKeysMap(
			final FederationManager<RS> federationManager,
			final List<SecretAuthenticationKey> authenticationData) {

		BiMap<RS, List<SecretAuthenticationKey>> map = HashBiMap.create(federationManager.getEndpoints().size());

		for (SecretAuthenticationKey secretAuthenticationKey : authenticationData) {

			RS rs = federationManager.getEndpointByUrnPrefix(secretAuthenticationKey.getUrnPrefix());

			List<SecretAuthenticationKey> keys = map.get(rs);
			if (keys == null) {
				keys = new LinkedList<SecretAuthenticationKey>();
				map.put(rs, keys);
			}
			keys.add(secretAuthenticationKey);
		}

		return map;
	}
}
