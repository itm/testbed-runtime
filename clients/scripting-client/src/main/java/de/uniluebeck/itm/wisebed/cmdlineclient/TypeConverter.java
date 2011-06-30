package de.uniluebeck.itm.wisebed.cmdlineclient;

import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.snaa.AuthenticationTriple;
import eu.wisebed.api.snaa.SecretAuthenticationKey;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public abstract class TypeConverter {

	public static List<AuthenticationTriple> convertCredentials(
			final AuthenticationCredentials... authenticationCredentialsList) {

		final List<AuthenticationTriple> authenticationTripleList = newArrayList();

		for (AuthenticationCredentials authenticationCredentials : authenticationCredentialsList) {

			final AuthenticationTriple authenticationTriple = new AuthenticationTriple();
			authenticationTriple.setUrnPrefix(authenticationCredentials.getUrnPrefix());
			authenticationTriple.setPassword(authenticationCredentials.getPassword());
			authenticationTriple.setUsername(authenticationCredentials.getUsername());
			authenticationTripleList.add(authenticationTriple);
		}

		return authenticationTripleList;
	}

	public static List<AuthenticationKey> convertAuthenticationKeys(
			final List<SecretAuthenticationKey> secretAuthenticationKeyList) {

		final List<AuthenticationKey> authenticationKeyList = newArrayList();

		for (SecretAuthenticationKey secretAuthenticationKey : secretAuthenticationKeyList) {
			final AuthenticationKey authenticationKey = new AuthenticationKey(
					secretAuthenticationKey.getUrnPrefix(),
					secretAuthenticationKey.getUsername(),
					secretAuthenticationKey.getSecretAuthenticationKey()
			);
			authenticationKeyList.add(authenticationKey);
		}

		return authenticationKeyList;
	}

	public static List<SecretReservationKey> convertReservationKeysToSM(final ReservationKey... reservationKeyList) {

		List<SecretReservationKey> secretReservationKeyList = newArrayList();

		for (ReservationKey reservationKey : reservationKeyList) {
			final SecretReservationKey secretReservationKey = new SecretReservationKey();
			secretReservationKey.setSecretReservationKey(reservationKey.getSecretReservationKey());
			secretReservationKey.setUrnPrefix(reservationKey.getUrnPrefix());
			secretReservationKeyList.add(secretReservationKey);
		}

		return secretReservationKeyList;
	}

	public static List<eu.wisebed.api.rs.SecretAuthenticationKey> convertAuthenticationKeysToRS(
			final AuthenticationKey[] authenticationKeys) {

		final List<eu.wisebed.api.rs.SecretAuthenticationKey> secretAuthenticationKeyList = newArrayList();

		for (AuthenticationKey authenticationKey : authenticationKeys) {
			eu.wisebed.api.rs.SecretAuthenticationKey secretAuthenticationKey = new eu.wisebed.api.rs.SecretAuthenticationKey();
			secretAuthenticationKey.setSecretAuthenticationKey(authenticationKey.getSecretAuthenticationKey());
			secretAuthenticationKey.setUrnPrefix(authenticationKey.getUrnPrefix());
			secretAuthenticationKey.setUsername(authenticationKey.getUsername());
			secretAuthenticationKeyList.add(secretAuthenticationKey);
		}

		return secretAuthenticationKeyList;
	}
}
