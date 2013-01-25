package de.uniluebeck.itm.tr.rs.singleurnprefix;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.rs.AuthorizationRequired;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Testbed Runtime internal implementation of the interface which defines the reservation system
 * (RS) for the WISEBED experimental facilities stripped of the Web Service accessibility.
 */
public class SingleUrnPrefixRS implements RS {

	private static final Logger log = LoggerFactory.getLogger(SingleUrnPrefixRS.class);

	@Inject
	@Named("SingleUrnPrefixSOAPRS.urnPrefix")
	private NodeUrnPrefix urnPrefix;

	@Inject
	private RSPersistence persistence;

	@Inject
	@Nullable
	@Named("SingleUrnPrefixSOAPRS.servedNodeUrns")
	private Provider<NodeUrn[]> servedNodeUrns;

	private List<SecretReservationKey> makeReservationInternal(
			final List<SecretAuthenticationKey> secretAuthenticationKeys,
			final List<NodeUrn> nodeUrns,
			final DateTime from,
			final DateTime to) throws RSFault_Exception {

		ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(from);
		crd.setTo(to);
		crd.getNodeUrns().addAll(nodeUrns);

		ConfidentialReservationDataKey data = new ConfidentialReservationDataKey();
		data.setUrnPrefix(secretAuthenticationKeys.get(0).getUrnPrefix());
		data.setUsername(secretAuthenticationKeys.get(0).getUsername());

		crd.getKeys().add(data);

		try {

			SecretReservationKey secretReservationKey = persistence.addReservation(crd, urnPrefix);

			data.setSecretReservationKey(secretReservationKey.getSecretReservationKey());

			List<SecretReservationKey> keys = new ArrayList<SecretReservationKey>();
			keys.add(secretReservationKey);
			return keys;

		} catch (Exception e) {
			throw createRSFault_Exception(e.getMessage());
		}
	}

	@Override
	public List<ConfidentialReservationData> getReservation(List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception,
			ReservationNotFoundFault_Exception {

		checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");
		checkArgumentValidReservation(secretReservationKeys);

		SecretReservationKey secretReservationKey = secretReservationKeys.get(0);
		ConfidentialReservationData reservation = persistence.getReservation(secretReservationKey);

		if (reservation == null) {
			String msg = "Reservation not found for key " + secretReservationKey;
			ReservationNotFoundFault exception = new ReservationNotFoundFault();
			exception.setMessage(msg);
			throw new ReservationNotFoundFault_Exception(msg, exception);
		}

		List<ConfidentialReservationData> res = new LinkedList<ConfidentialReservationData>();
		res.add(reservation);
		return res;
	}

	@Override
	@AuthorizationRequired("RS_DELETE_RESERVATION")
	public void deleteReservation(List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception, ReservationNotFoundFault_Exception {

		checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");

		checkArgumentValidReservation(secretReservationKeys);

		// TODO check authentication (https://github.com/itm/testbed-runtime/issues/47)
		// checkArgumentValidAuthentication(authenticationData);
		// try {
		// checkAuthorization(authenticationData.get(0), Actions.DELETE_RESERVATION);
		// } catch (AuthorizationFault_Exception e) {
		// throwRSFault_Exception(e);
		// }

		SecretReservationKey secretReservationKeyToDelete = secretReservationKeys.get(0);
		ConfidentialReservationData reservationToDelete = persistence.getReservation(secretReservationKeyToDelete);

		checkNotAlreadyStarted(reservationToDelete);

		persistence.deleteReservation(secretReservationKeyToDelete);

		log.debug("Deleted reservation {}", reservationToDelete);
	}

	@Override
	@AuthorizationRequired("RS_MAKE_RESERVATION")
	public List<SecretReservationKey> makeReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
													  final List<NodeUrn> nodeUrns,
													  final DateTime from,
													  final DateTime to)
			throws AuthorizationFault_Exception, RSFault_Exception, ReservationConflictFault_Exception {

		checkArgumentValid(nodeUrns, from, to);
		checkArgumentValidAuthentication(secretAuthenticationKeys);
		checkNodesServed(nodeUrns);
		checkNodesAvailable(newHashSet(nodeUrns), from, to);
		return makeReservationInternal(secretAuthenticationKeys, nodeUrns, from, to);
	}

	private void checkNotAlreadyStarted(final ConfidentialReservationData reservation) throws RSFault_Exception {

		DateTime reservationFrom = new DateTime(reservation.getFrom().toGregorianCalendar());

		if (reservationFrom.isBeforeNow()) {
			final String msg = "You are not allowed to delete reservations that have already started.";
			throw createRSFault_Exception(msg);
		}
	}

	@Override
	public List<PublicReservationData> getReservations(DateTime from, DateTime to)
			throws RSFault_Exception {

		Preconditions.checkNotNull(from, "Parameter from date is null or empty");
		Preconditions.checkNotNull(to, "Parameter to date is null or empty");

		Interval request =
				new Interval(new DateTime(from.toGregorianCalendar()), new DateTime(to.toGregorianCalendar()));
		List<PublicReservationData> res = convertToPublic(persistence.getReservations(request));

		log.debug("Found " + res.size() + " reservations from " + from + " until " + to);
		return res;
	}

	@Override
	@AuthorizationRequired("RS_GET_RESERVATIONS")
	public List<ConfidentialReservationData> getConfidentialReservations(
			List<SecretAuthenticationKey> secretAuthenticationKey,
			DateTime from,
			DateTime to)
			throws RSFault_Exception {

		checkNotNull(from, "Parameter from is null!");
		checkNotNull(to, "Parameter to is null!");
		checkNotNull(secretAuthenticationKey, "Parameter secretAuthenticationKeys is null!");

		checkArgumentValid(from, to);
		checkArgumentValidAuthentication(secretAuthenticationKey);

		SecretAuthenticationKey key = secretAuthenticationKey.get(0);

		Interval interval = new Interval(new DateTime(from.toGregorianCalendar()),
				new DateTime(to.toGregorianCalendar())
		);

		List<ConfidentialReservationData> reservationsOfAllUsersInInterval = persistence.getReservations(interval);
		List<ConfidentialReservationData> reservationsOfAuthenticatedUserInInterval = newArrayList();

		for (ConfidentialReservationData crd : reservationsOfAllUsersInInterval) {
			boolean sameUser = crd.getKeys().get(0).getUsername().equals(key.getUsername());
			if (sameUser) {
				reservationsOfAuthenticatedUserInInterval.add(crd);
			}
		}

		return reservationsOfAuthenticatedUserInInterval;
	}

	public void checkArgumentValidAuthentication(List<SecretAuthenticationKey> authenticationData)
			throws RSFault_Exception {

		// Check if authentication data has been supplied
		if (authenticationData == null || authenticationData.size() != 1) {
			String msg = "No or too much authentication data supplied -> error.";
			log.warn(msg);
			RSFault exception = new RSFault();
			exception.setMessage(msg);
			throw new RSFault_Exception(msg, exception);
		}

		SecretAuthenticationKey sak = authenticationData.get(0);
		if (!urnPrefix.equals(sak.getUrnPrefix())) {
			String msg = "Not serving urn prefix " + sak.getUrnPrefix();
			log.warn(msg);
			RSFault exception = new RSFault();
			exception.setMessage(msg);
			throw new RSFault_Exception(msg, exception);
		}
	}

	private RSFault_Exception createRSFault_Exception(String message) {
		RSFault exception = new RSFault();
		exception.setMessage(message);
		return new RSFault_Exception(message, exception);
	}

	private PublicReservationData convertToPublic(ConfidentialReservationData confidentialReservationData) {
		PublicReservationData publicReservationData = new PublicReservationData();
		publicReservationData.setFrom(confidentialReservationData.getFrom());
		publicReservationData.setTo(confidentialReservationData.getTo());
		publicReservationData.getNodeUrns().addAll(confidentialReservationData.getNodeUrns());
		return publicReservationData;
	}

	private void checkNodesServed(List<NodeUrn> nodeUrns) throws RSFault_Exception {

		// Check if we serve all node urns by urnPrefix
		for (NodeUrn nodeUrn : nodeUrns) {
			if (!nodeUrn.belongsTo(urnPrefix)) {
				throw createRSFault_Exception(
						"Not responsible for node URN " + nodeUrn + ", only serving prefix: " + urnPrefix
				);
			}
		}

		// Ask Session Management Endpoint of the testbed we're responsible for for it's network
		// description
		// and check if the individual node urns of the reservation are existing
		if (servedNodeUrns != null && servedNodeUrns.get() != null) {

			NodeUrn[] networkNodes;
			try {
				networkNodes = servedNodeUrns.get();
			} catch (Exception e) {
				throw createRSFault_Exception(e.getMessage());
			}

			List<NodeUrn> unservedNodes = newArrayList();

			boolean contained;
			for (NodeUrn nodeUrn : nodeUrns) {

				contained = false;

				for (NodeUrn networkNode : networkNodes) {
					if (networkNode.equals(nodeUrn)) {
						contained = true;
					}
				}

				if (!contained) {
					unservedNodes.add(nodeUrn);
				}
			}

			if (unservedNodes.size() > 0) {
				throw createRSFault_Exception("The node URNs " + Arrays.toString(unservedNodes.toArray())
						+ " are unknown to the reservation system!"
				);
			}

		} else {
			log.debug("Not checking session management endpoint for node URN validity as no endpoint is configured.");
		}

	}

	private void checkArgumentValid(final List<NodeUrn> nodeUrns,
									final DateTime fromArg,
									final DateTime toArg) throws RSFault_Exception {

		try {

			checkNotNull(fromArg, "Reservation start time parameter \"from\" is missing.");
			checkNotNull(toArg, "Reservation end time parameter \"to\" is missing.");

			final DateTime from = new DateTime(fromArg.toGregorianCalendar());
			final DateTime to = new DateTime(toArg.toGregorianCalendar());

			checkArgument(!to.isBeforeNow(), "Reservation end time parameter \"to\" lies in the past.");
			checkArgument(!to.isBefore(from), "Reservation end time is before reservation start time.");
			checkArgument(!nodeUrns.isEmpty(), "Empty reservation request! At least one node must be reserved.");

		} catch (Exception e) {
			log.debug(e.getMessage());
			RSFault exception = new RSFault();
			exception.setMessage(e.getMessage());
			throw new RSFault_Exception(e.getMessage(), exception);
		}
	}

	private void checkArgumentValidReservation(List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception {

		String msg = null;
		SecretReservationKey srk;

		// Check if reservation data has been supplied
		if (secretReservationKeys == null || secretReservationKeys.size() != 1) {
			msg = "No or too much secretReservationKeys supplied -> error.";

		} else {
			srk = secretReservationKeys.get(0);
			if (!urnPrefix.equals(srk.getUrnPrefix())) {
				msg = "Not serving urn prefix " + srk.getUrnPrefix();
			}
		}

		if (msg != null) {
			log.warn(msg);
			RSFault exception = new RSFault();
			exception.setMessage(msg);
			throw new RSFault_Exception(msg, exception);
		}
	}

	private void checkNodesAvailable(final Set<NodeUrn> nodeUrns,
									 final DateTime from,
									 final DateTime to)
			throws ReservationConflictFault_Exception, RSFault_Exception {

		Set<NodeUrn> reservedNodeUrns = newHashSet();
		for (PublicReservationData res : getReservations(from, to)) {
			reservedNodeUrns.addAll(res.getNodeUrns());
		}

		final Sets.SetView<NodeUrn> intersection = Sets.intersection(reservedNodeUrns, nodeUrns);

		if (!intersection.isEmpty()) {
			final String reservedNodesString = Arrays.toString(intersection.toArray());
			final String msg = "Some of the nodes are reserved during the requested time (" + reservedNodesString + ")";
			log.warn(msg);
			ReservationConflictFault exception = new ReservationConflictFault();
			exception.setMessage(msg);
			exception.getReservedNodeUrns().addAll(intersection);
			throw new ReservationConflictFault_Exception(msg, exception);
		}
	}

	private void checkArgumentValid(final DateTime from, final DateTime to)
			throws RSFault_Exception {

		if (from == null || to == null) {
			String message = "Error on checking null for period. Either period, period.from or period.to is null.";
			log.warn(message);
			RSFault rse = new RSFault();
			rse.setMessage(message);
			throw new RSFault_Exception(message, rse);
		}
	}

	private List<PublicReservationData> convertToPublic(
			List<ConfidentialReservationData> confidentialReservationDataList) {
		List<PublicReservationData> publicReservationDataList = Lists.newArrayList();
		for (ConfidentialReservationData confidentialReservationData : confidentialReservationDataList) {
			publicReservationDataList.add(convertToPublic(confidentialReservationData));
		}
		return publicReservationDataList;
	}

}
