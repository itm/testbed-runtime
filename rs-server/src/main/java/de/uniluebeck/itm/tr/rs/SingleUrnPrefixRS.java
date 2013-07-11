package de.uniluebeck.itm.tr.rs;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import eu.wisebed.api.v3.snaa.ValidationResult;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
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

	private final CommonConfig commonConfig;

	private final RSPersistence persistence;

	private final ServedNodeUrnsProvider servedNodeUrnsProvider;

	private final SNAA snaa;

	@Inject
	public SingleUrnPrefixRS(final CommonConfig commonConfig,
							 final RSPersistence persistence,
							 final ServedNodeUrnsProvider servedNodeUrnsProvider,
							 final SNAA snaa) {
		this.commonConfig = checkNotNull(commonConfig);
		this.persistence = checkNotNull(persistence);
		this.servedNodeUrnsProvider = checkNotNull(servedNodeUrnsProvider);
		this.snaa = checkNotNull(snaa);
	}

	@Override
	public List<PublicReservationData> getReservations(
			final DateTime from,
			final DateTime to,
			final Integer offset,
			final Integer amount)
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
	public List<ConfidentialReservationData> getConfidentialReservations(
			final List<SecretAuthenticationKey> secretAuthenticationKeys,
			final DateTime from,
			final DateTime to,
			final Integer offset,
			final Integer amount)
			throws AuthorizationFault, RSFault_Exception, AuthenticationFault {

		checkNotNull(from, "Parameter from is null!");
		checkNotNull(to, "Parameter to is null!");
		checkNotNull(secretAuthenticationKeys, "Parameter secretAuthenticationKeys is null!");

		checkArgumentValid(from, to);
		checkArgumentValidAuthentication(secretAuthenticationKeys);
		checkValidityWithSNAA(secretAuthenticationKeys);

		SecretAuthenticationKey key = secretAuthenticationKeys.get(0);

		Interval interval = new Interval(new DateTime(from.toGregorianCalendar()),
				new DateTime(to.toGregorianCalendar())
		);

		List<ConfidentialReservationData> reservationsOfAllUsersInInterval = persistence.getReservations(interval);
		List<ConfidentialReservationData> reservationsOfAuthenticatedUserInInterval = newArrayList();

		for (ConfidentialReservationData crd : reservationsOfAllUsersInInterval) {
			boolean sameUser = crd.getUsername().equals(key.getUsername());
			if (sameUser) {
				reservationsOfAuthenticatedUserInInterval.add(crd);
			}
		}

		return reservationsOfAuthenticatedUserInInterval;
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
	public void deleteReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
								  final List<SecretReservationKey> secretReservationKeys)
			throws AuthenticationFault, AuthorizationFault, RSFault_Exception, UnknownSecretReservationKeyFault {

		checkNotNull(secretAuthenticationKeys, "Parameter secretAuthenticationKeys is null!");
		checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");

		checkArgumentValidAuthentication(secretAuthenticationKeys);
		checkArgumentValidReservation(secretReservationKeys);
		checkValidityWithSNAA(secretAuthenticationKeys);

		SecretReservationKey secretReservationKeyToDelete = secretReservationKeys.get(0);
		ConfidentialReservationData reservationToDelete = persistence.getReservation(secretReservationKeyToDelete);

		checkNotAlreadyStarted(reservationToDelete);

		persistence.deleteReservation(secretReservationKeyToDelete);

		log.debug("Deleted reservation {}", reservationToDelete);
	}

	@Override
	public List<SecretReservationKey> makeReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
													  final List<NodeUrn> nodeUrns,
													  final DateTime from,
													  final DateTime to,
													  final String description,
													  final List<KeyValuePair> options)
			throws AuthorizationFault, RSFault_Exception, ReservationConflictFault_Exception {

		checkArgumentValid(nodeUrns, from, to);
		checkArgumentValidAuthentication(secretAuthenticationKeys);
		checkNodesServed(nodeUrns);
		checkNodesAvailable(newHashSet(nodeUrns), from, to, null, null);

		final ConfidentialReservationData confidentialReservationData = persistence.addReservation(
				nodeUrns,
				from,
				to,
				secretAuthenticationKeys.get(0).getUsername(),
				secretAuthenticationKeys.get(0).getUrnPrefix(),
				description,
				options
		);

		return newArrayList(confidentialReservationData.getSecretReservationKey());
	}

	private void checkNotAlreadyStarted(final ConfidentialReservationData reservation) throws RSFault_Exception {

		DateTime reservationFrom = new DateTime(reservation.getFrom().toGregorianCalendar());

		if (reservationFrom.isBeforeNow()) {
			final String msg = "You are not allowed to delete reservations that have already started.";
			throw createRSFault_Exception(msg);
		}
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
		if (!commonConfig.getUrnPrefix().equals(sak.getUrnPrefix())) {
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
			if (!nodeUrn.belongsTo(commonConfig.getUrnPrefix())) {
				throw createRSFault_Exception(
						"Not responsible for node URN " + nodeUrn + ", only serving prefix: " + commonConfig
								.getUrnPrefix()
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
			if (!commonConfig.getUrnPrefix().equals(srk.getUrnPrefix())) {
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
									 final DateTime to,
									 final Integer offset,
									 final Integer amount)
			throws ReservationConflictFault_Exception, RSFault_Exception {

		Set<NodeUrn> reservedNodeUrns = newHashSet();
		for (PublicReservationData res : getReservations(from, to, offset, amount)) {
			reservedNodeUrns.addAll(res.getNodeUrns());
		}

		final Sets.SetView<NodeUrn> intersection = Sets.intersection(reservedNodeUrns, nodeUrns);

		if (!intersection.isEmpty()) {
			final String reservedNodesString = Arrays.toString(intersection.toArray());
			final String msg = "Some of the nodes are reserved during the requested time (" + reservedNodesString + ")";
			log.debug(msg);
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

	private void checkValidityWithSNAA(final List<SecretAuthenticationKey> secretAuthenticationKeys)
			throws AuthenticationFault {
		try {
			for (ValidationResult validationResult : snaa.isValid(secretAuthenticationKeys)) {
				if (!validationResult.isValid()) {
					final String msg = "SecretAuthenticationKeys are invalid or timed out!";
					final eu.wisebed.api.v3.common.AuthenticationFault faultInfo =
							new eu.wisebed.api.v3.common.AuthenticationFault();
					faultInfo.setMessage(msg);
					throw new AuthenticationFault(msg, faultInfo);
				}
			}

		} catch (SNAAFault_Exception e) {
			throw propagate(e);
		}
	}
}
