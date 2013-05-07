package de.uniluebeck.itm.tr.rs;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import eu.wisebed.api.v3.rs.RS;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static eu.wisebed.api.v3.WisebedServiceHelper.createRSUnknownSecretReservationKeyFault;

/**
 * Testbed Runtime internal implementation of the interface which defines the reservation system
 * (RS) for the WISEBED experimental facilities stripped of the Web Service accessibility.
 */
public class SingleUrnPrefixRS implements RS {

	private static final Logger log = LoggerFactory.getLogger(SingleUrnPrefixRS.class);

	private final RSConfig config;

	private final RSPersistence persistence;

	private final Provider<Set<NodeUrn>> servedNodeUrnsProvider;

	@Inject
	public SingleUrnPrefixRS(final RSConfig config,
							 final RSPersistence persistence,
							 final ServedNodeUrnsProvider servedNodeUrnsProvider) {
		this.config = checkNotNull(config);
		this.persistence = checkNotNull(persistence);
		this.servedNodeUrnsProvider = checkNotNull(servedNodeUrnsProvider);
	}

	@Override
	public List<ConfidentialReservationData> getReservation(List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception,
			UnknownSecretReservationKeyFault {

		checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");
		checkArgumentValidReservation(secretReservationKeys);

		SecretReservationKey secretReservationKey = secretReservationKeys.get(0);
		ConfidentialReservationData reservation = persistence.getReservation(secretReservationKey);

		if (reservation == null) {
			throw createRSUnknownSecretReservationKeyFault("Reservation not found", secretReservationKey);
		}

		List<ConfidentialReservationData> res = new LinkedList<ConfidentialReservationData>();
		res.add(reservation);
		return res;
	}

	@Override
	@AuthorizationRequired("RS_DELETE_RESERVATION")
	public void deleteReservation(List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {

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
													  final DateTime to,
													  final String description,
													  final List<KeyValuePair> options)
			throws AuthorizationFault_Exception, RSFault_Exception, ReservationConflictFault_Exception {

		checkArgumentValid(nodeUrns, from, to);
		checkArgumentValidAuthentication(secretAuthenticationKeys);
		checkNodesServed(nodeUrns);
		checkNodesAvailable(newHashSet(nodeUrns), from, to);
		return makeReservationInternal(secretAuthenticationKeys, nodeUrns, from, to, description, options);
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
		if (!config.getUrnPrefix().equals(sak.getUrnPrefix())) {
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
			if (!nodeUrn.belongsTo(config.getUrnPrefix())) {
				throw createRSFault_Exception(
						"Not responsible for node URN " + nodeUrn + ", only serving prefix: " + config.getUrnPrefix()
				);
			}
		}

		Set<NodeUrn> servedNodeUrns;
		try {
			servedNodeUrns = servedNodeUrnsProvider.get();
		} catch (Exception e) {
			throw createRSFault_Exception(e.getMessage());
		}

		List<NodeUrn> unservedNodes = newArrayList(filter(nodeUrns, Predicates.not(Predicates.in(servedNodeUrns))));

		boolean contained;
		for (NodeUrn nodeUrn : nodeUrns) {

			contained = false;

			for (NodeUrn networkNode : servedNodeUrns) {
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
			if (!config.getUrnPrefix().equals(srk.getUrnPrefix())) {
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

	private List<SecretReservationKey> makeReservationInternal(
			final List<SecretAuthenticationKey> secretAuthenticationKeys,
			final List<NodeUrn> nodeUrns,
			final DateTime from,
			final DateTime to,
			final String description,
			final List<KeyValuePair> options) throws RSFault_Exception {

		ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(from);
		crd.setTo(to);
		crd.getNodeUrns().addAll(nodeUrns);
		crd.setDescription(description);
		crd.getOptions().addAll(options);

		ConfidentialReservationDataKey data = new ConfidentialReservationDataKey();
		data.setUrnPrefix(secretAuthenticationKeys.get(0).getUrnPrefix());
		data.setUsername(secretAuthenticationKeys.get(0).getUsername());

		crd.getKeys().add(data);

		try {

			SecretReservationKey secretReservationKey = persistence.addReservation(crd, config.getUrnPrefix());

			data.setKey(secretReservationKey.getKey());

			List<SecretReservationKey> keys = new ArrayList<SecretReservationKey>();
			keys.add(secretReservationKey);
			return keys;

		} catch (Exception e) {
			throw createRSFault_Exception(e.getMessage());
		}
	}
}
