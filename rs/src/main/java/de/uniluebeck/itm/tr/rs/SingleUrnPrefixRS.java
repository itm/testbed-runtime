package de.uniluebeck.itm.tr.rs;

import com.google.common.base.Joiner;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.NodeUrnPrefixHelper.SAK_TO_NODE_URN_PREFIX;
import static de.uniluebeck.itm.tr.common.NodeUrnPrefixHelper.SRK_TO_NODE_URN_PREFIX;
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
			@Nullable final DateTime from,
			@Nullable final DateTime to,
			@Nullable final Integer offset,
			@Nullable final Integer amount,
			@Nullable final Boolean showCancelled)
			throws RSFault_Exception {

		List<PublicReservationData> res = convertToPublic(
				persistence.getReservations(from, to, offset, amount, showCancelled)
		);

		log.debug(
				"Found " + res.size() + " reservations from " + from + " until " + to + " (showCancelled={})",
				showCancelled
		);

		return res;
	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			final List<SecretAuthenticationKey> secretAuthenticationKeys,
			@Nullable final DateTime from,
			@Nullable final DateTime to,
			@Nullable final Integer offset,
			@Nullable final Integer amount,
			@Nullable final Boolean showCancelled)
			throws AuthorizationFault, RSFault_Exception, AuthenticationFault {

		checkNotNull(secretAuthenticationKeys, "Parameter secretAuthenticationKeys is null!");
		final SecretAuthenticationKey relevantSAK = getRelevantSAK(secretAuthenticationKeys);
		checkValidityWithSNAA(relevantSAK);

		final List<ConfidentialReservationData> allUsersInInterval =
				persistence.getReservations(from, to, offset, amount, showCancelled);
		final List<ConfidentialReservationData> authenticatedUserInInterval = newArrayList();

		for (ConfidentialReservationData crd : allUsersInInterval) {
			boolean sameUser = crd.getUsername().equals(relevantSAK.getUsername());
			if (sameUser) {
				authenticatedUserInInterval.add(crd);
			}
		}

		return authenticatedUserInInterval;
	}

	@Override
	public List<ConfidentialReservationData> getReservation(List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception,
			UnknownSecretReservationKeyFault {

		checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");
		final List<SecretReservationKey> relevantSRKs = getRelevantSRKs(secretReservationKeys);
		final List<ConfidentialReservationData> res = newArrayList();

		for (SecretReservationKey relevantSRK : relevantSRKs) {
			ConfidentialReservationData reservation = persistence.getReservation(relevantSRK);

			if (reservation == null) {
				throw createRSUnknownSecretReservationKeyFault("Reservation not found", relevantSRK);
			}

			res.add(reservation);
		}

		return res;
	}

	@Override
	public void cancelReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
								  final List<SecretReservationKey> secretReservationKeys)
			throws AuthenticationFault, AuthorizationFault, RSFault_Exception, UnknownSecretReservationKeyFault {

		checkNotNull(secretAuthenticationKeys, "Parameter secretAuthenticationKeys is null!");
		checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");

		// find relevant SecretAuthenticationKey and check it's validity, ignore the others
		checkValidityWithSNAA(getRelevantSAK(secretAuthenticationKeys));

		// find relevant SecretReservationKey, ignore the others
		final List<SecretReservationKey> relevantSRKs = getRelevantSRKs(secretReservationKeys);

		for (SecretReservationKey relevantSRK : relevantSRKs) {
			ConfidentialReservationData reservationToDelete = persistence.getReservation(relevantSRK);
			checkNotInPast(reservationToDelete);
			persistence.cancelReservation(relevantSRK);
			log.debug("Cancelled reservation {}", reservationToDelete);
		}
	}

	@Override
	public List<SecretReservationKey> makeReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
													  final List<NodeUrn> nodeUrns,
													  final DateTime from,
													  final DateTime to,
													  final String description,
													  final List<KeyValuePair> options)
			throws AuthorizationFault, RSFault_Exception, ReservationConflictFault_Exception, AuthenticationFault {

		// find relevant SecretAuthenticationKey and check it's validity, ignore the others
		final SecretAuthenticationKey relevantSAK = getRelevantSAK(secretAuthenticationKeys);
		checkValidityWithSNAA(relevantSAK);

		checkArgumentValid(nodeUrns, from, to);
		checkNodesServed(nodeUrns);
		checkNodesAvailable(newHashSet(nodeUrns), from, to, null, null);

		final ConfidentialReservationData confidentialReservationData = persistence.addReservation(
				nodeUrns,
				from,
				to,
				relevantSAK.getUsername(),
				commonConfig.getUrnPrefix(),
				description,
				options
		);

		return newArrayList(confidentialReservationData.getSecretReservationKey());
	}

	private void checkNotInPast(final ConfidentialReservationData reservation) throws RSFault_Exception {
		if (reservation.getTo().isBeforeNow()) {
			final String msg = "You are not allowed to cancel reservations that have already ended.";
			throw createRSFault_Exception(msg);
		}
	}

	public SecretAuthenticationKey getRelevantSAK(List<SecretAuthenticationKey> authenticationData)
			throws RSFault_Exception {

		final List<SecretAuthenticationKey> relevantSAKs = newArrayList();
		for (SecretAuthenticationKey sak : authenticationData) {
			if (commonConfig.getUrnPrefix().equals(sak.getUrnPrefix())) {
				relevantSAKs.add(sak);
			}
		}

		if (relevantSAKs.size() == 1) {
			return relevantSAKs.get(0);
		}

		final String prefixes =
				Joiner.on(",").join(newHashSet(transform(authenticationData, SAK_TO_NODE_URN_PREFIX)));
		final String msg;
		if (relevantSAKs.size() > 1) {
			msg = "Too many SecretAuthenticationKeys with the same URN prefix given ("
					+ prefixes
					+ "). Can't decide which one to choose.";
		} else {
			msg = "The supplied secret authentication keys (" + prefixes + ") don't contain keys for this "
					+ "testbed (URN prefix \"" + commonConfig.getUrnPrefix() + "\").";
		}

		log.warn(msg);
		RSFault exception = new RSFault();
		exception.setMessage(msg);
		throw new RSFault_Exception(msg, exception);
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
									final DateTime from,
									final DateTime to) throws RSFault_Exception {

		try {

			checkNotNull(from, "Reservation start time parameter \"from\" is missing.");
			checkNotNull(to, "Reservation end time parameter \"to\" is missing.");

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

	private List<SecretReservationKey> getRelevantSRKs(List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception {

		final List<SecretReservationKey> relevantSRKs = newArrayList();

		for (SecretReservationKey secretReservationKey : secretReservationKeys) {
			if (commonConfig.getUrnPrefix().equals(secretReservationKey.getUrnPrefix())) {
				relevantSRKs.add(secretReservationKey);
			}
		}

		if (!relevantSRKs.isEmpty()) {
			return relevantSRKs;
		}

		final String prefixes =
				Joiner.on(",").join(newHashSet(transform(secretReservationKeys, SRK_TO_NODE_URN_PREFIX)));
		final String msg = "The supplied secret reservation keys (" + prefixes + ") don't contain keys for this "
				+ "testbed (URN prefix \"" + commonConfig.getUrnPrefix() + "\").";

		log.warn(msg);
		RSFault exception = new RSFault();
		exception.setMessage(msg);
		throw new RSFault_Exception(msg, exception);
	}

	private void checkNodesAvailable(final Set<NodeUrn> nodeUrns,
									 final DateTime from,
									 final DateTime to,
									 final Integer offset,
									 final Integer amount)
			throws ReservationConflictFault_Exception, RSFault_Exception {

		Set<NodeUrn> reservedNodeUrns = newHashSet();
		for (PublicReservationData res : getReservations(from, to, offset, amount, false)) {
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

	private List<PublicReservationData> convertToPublic(
			List<ConfidentialReservationData> confidentialReservationDataList) {
		List<PublicReservationData> publicReservationDataList = Lists.newArrayList();
		for (ConfidentialReservationData confidentialReservationData : confidentialReservationDataList) {
			publicReservationDataList.add(convertToPublic(confidentialReservationData));
		}
		return publicReservationDataList;
	}

	private void checkValidityWithSNAA(final SecretAuthenticationKey secretAuthenticationKey)
			throws AuthenticationFault {
		try {
			for (ValidationResult validationResult : snaa.isValid(newArrayList(secretAuthenticationKey))) {
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
