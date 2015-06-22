/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.rs.persistence.jpa;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistenceListener;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.ReservationDataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.SecretReservationKeyInternal;
import de.uniluebeck.itm.util.SecureIdGenerator;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.xml.datatype.DatatypeConfigurationException;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.rs.persistence.jpa.TypeConverter.convertConfidentialReservationData;
import static eu.wisebed.api.v3.WisebedServiceHelper.createRSUnknownSecretReservationKeyFault;
import static org.joda.time.DateTimeZone.forTimeZone;

public class RSPersistenceJPA implements RSPersistence {

	private static final Logger log = LoggerFactory.getLogger(RSPersistence.class);

	private final Provider<EntityManager> em;

	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private final TimeZone localTimeZone;

	private final Object listenersLock = new Object();

	private ImmutableSet<RSPersistenceListener> listeners = ImmutableSet.of();

	@Inject
	public RSPersistenceJPA(final Provider<EntityManager> em, final TimeZone timeZone) {
		this.em = checkNotNull(em);
		this.localTimeZone = checkNotNull(timeZone);
	}

	@Override
	@Transactional
	public ConfidentialReservationData addReservation(final List<NodeUrn> nodeUrns,
													  final DateTime from,
													  final DateTime to,
													  final String username,
													  final NodeUrnPrefix urnPrefix,
													  final String description,
													  final List<KeyValuePair> options) throws RSFault_Exception {

		SecretReservationKeyInternal secretReservationKeyInternal;
		String generatedSecretReservationKey;

		generatedSecretReservationKey = secureIdGenerator.getNextId();

		secretReservationKeyInternal = new SecretReservationKeyInternal();
		secretReservationKeyInternal.setSecretReservationKey(generatedSecretReservationKey);
		secretReservationKeyInternal.setUrnPrefix(urnPrefix.toString());

		em.get().persist(secretReservationKeyInternal);

		final SecretReservationKey secretReservationKey = new SecretReservationKey();
		secretReservationKey.setUrnPrefix(urnPrefix);
		secretReservationKey.setKey(generatedSecretReservationKey);

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(from);
		crd.setTo(to);
		crd.getNodeUrns().addAll(nodeUrns);
		crd.setDescription(description);
		crd.getOptions().addAll(options);
		crd.setUsername(username);
		crd.setSecretReservationKey(secretReservationKey);

		ReservationDataInternal reservationData = new ReservationDataInternal(
				secretReservationKeyInternal,
				TypeConverter.convert(crd, localTimeZone),
				urnPrefix.toString()
		);

		em.get().persist(reservationData);

		final ConfidentialReservationData data;
		try {
			data = TypeConverter.convert(reservationData.getConfidentialReservationData(), localTimeZone);
		} catch (DatatypeConfigurationException e) {
			String msg = "Could not add Reservation because of: " + e.getMessage();
			log.error(msg);
			RSFault exception = new RSFault();
			exception.setMessage(msg);
			throw new RSFault_Exception(msg, exception, e);
		}

		for (RSPersistenceListener listener : listeners) {
			try {
				listener.onReservationMade(newArrayList(data));
			} catch (Exception e) {
				log.error("Listener threw exception while being notified of newly created reservation: {}", e);
			}
		}

		return crd;
	}

	@Override
	@Transactional
	public ConfidentialReservationData getReservation(SecretReservationKey srk)
			throws UnknownSecretReservationKeyFault, RSFault_Exception {
		Query query = em.get().createNamedQuery(ReservationDataInternal.QGetByReservationKey.QUERY_NAME);
		query.setParameter(ReservationDataInternal.QGetByReservationKey.P_SECRET_RESERVATION_KEY, srk.getKey());
		ReservationDataInternal reservationData;
		try {
			reservationData = (ReservationDataInternal) query.getSingleResult();
		} catch (NoResultException e) {
			throw createRSUnknownSecretReservationKeyFault("Reservation not found", srk);
		}
		try {
			return TypeConverter.convert(reservationData.getConfidentialReservationData(), this.localTimeZone);
		} catch (DatatypeConfigurationException e) {
			throw new RSFault_Exception(e.getMessage(), new RSFault());
		}
	}

	@Override
	@Transactional
	public ConfidentialReservationData cancelReservation(SecretReservationKey srk)
			throws UnknownSecretReservationKeyFault, RSFault_Exception {

		Query query = em.get().createNamedQuery(ReservationDataInternal.QGetByReservationKey.QUERY_NAME);
		query.setParameter(ReservationDataInternal.QGetByReservationKey.P_SECRET_RESERVATION_KEY, srk.getKey());
		ReservationDataInternal reservationData;
		try {
			reservationData = (ReservationDataInternal) query.getSingleResult();
		} catch (NoResultException e) {
			throw createRSUnknownSecretReservationKeyFault("Reservation not found", srk);
		}
		final long nowMillis = DateTime.now(DateTimeZone.forTimeZone(this.localTimeZone)).getMillis();

		if (reservationData.getConfidentialReservationData().getToDate() < nowMillis) {
			final String msg = "Reservation time span lies in the past and can't therefore be cancelled anymore";
			final RSFault faultInfo = new RSFault();
			faultInfo.setMessage(msg);
			throw new RSFault_Exception(msg, faultInfo);
		}

		reservationData.getConfidentialReservationData().setCancelledDate(nowMillis);
		em.get().persist(reservationData);

		final ConfidentialReservationData data;
		try {
			data = TypeConverter.convert(reservationData.getConfidentialReservationData(), this.localTimeZone);
		} catch (DatatypeConfigurationException e) {
			throw new RSFault_Exception(e.getMessage(), new RSFault());
		}

		for (RSPersistenceListener listener : listeners) {
			listener.onReservationCancelled(newArrayList(data));
		}

		return data;
	}

    @Override
    @Transactional
    public ConfidentialReservationData finalizeReservation(SecretReservationKey secretReservationKey) throws UnknownSecretReservationKeyFault, RSFault_Exception {
        Query query = em.get().createNamedQuery(ReservationDataInternal.QGetByReservationKey.QUERY_NAME);
        query.setParameter(ReservationDataInternal.QGetByReservationKey.P_SECRET_RESERVATION_KEY, secretReservationKey.getKey());
        ReservationDataInternal reservationData;
        try {
            reservationData = (ReservationDataInternal) query.getSingleResult();
        } catch (NoResultException e) {
            throw createRSUnknownSecretReservationKeyFault("Reservation not found", secretReservationKey);
        }
        final long nowMillis = DateTime.now(DateTimeZone.forTimeZone(this.localTimeZone)).getMillis();

        if (reservationData.getConfidentialReservationData().getToDate() > nowMillis && reservationData.getConfidentialReservationData().getCancelledDate() != null && reservationData.getConfidentialReservationData().getCancelledDate() > nowMillis) {
            final String msg = "Reservation time span lies in the future and can't therefore be finalized yet";
            final RSFault faultInfo = new RSFault();
            faultInfo.setMessage(msg);
            throw new RSFault_Exception(msg, faultInfo);
        }

        reservationData.getConfidentialReservationData().setFinalizedDate(nowMillis);
        em.get().persist(reservationData);

        final ConfidentialReservationData data;
        try {
            data = TypeConverter.convert(reservationData.getConfidentialReservationData(), this.localTimeZone);
        } catch (DatatypeConfigurationException e) {
            throw new RSFault_Exception(e.getMessage(), new RSFault());
        }

        for (RSPersistenceListener listener : listeners) {
            listener.onReservationFinalized(newArrayList(data));
        }

        return data;
    }

    @Override
	public void addListener(final RSPersistenceListener rsPersistenceListener) {
		synchronized (listenersLock) {
			listeners = ImmutableSet.<RSPersistenceListener>builder().addAll(listeners).add(rsPersistenceListener).build();
		}
	}

	@Override
	public void removeListener(final RSPersistenceListener listener) {
		final ImmutableSet.Builder<RSPersistenceListener> builder = ImmutableSet.builder();
		for (RSPersistenceListener old : listeners) {
			if (old != listener) {
				builder.add(old);
			}
		}
		synchronized (listenersLock) {
			listeners = builder.build();
		}
	}

	@Override
	@Transactional
	@SuppressWarnings({"unchecked", "ConstantConditions"})
	public List<ConfidentialReservationData> getReservations(@Nullable final DateTime from,
															 @Nullable final DateTime to,
															 @Nullable final Integer offset,
															 @Nullable final Integer amount,
															 @Nullable Boolean showCancelled)
			throws RSFault_Exception {

		if (showCancelled == null) {
			showCancelled = true; // default value
		}

		Query query;

		if (from == null && to == null) {

			if (showCancelled) {

				query = em.get().createNamedQuery(ReservationDataInternal.QGetAll.QUERY_NAME);

			} else {

				query = em.get().createNamedQuery(ReservationDataInternal.QGetAllWithoutCancelled.QUERY_NAME);
			}

		} else if (from == null && to != null) {

			DateTime localTo = to.toDateTime(forTimeZone(localTimeZone));
			if (showCancelled) {

				query = em.get().createNamedQuery(ReservationDataInternal.QGetTo.QUERY_NAME);
				query.setParameter(ReservationDataInternal.QGetTo.P_TO, localTo.getMillis());

			} else {

				query = em.get().createNamedQuery(ReservationDataInternal.QGetToWithoutCancelled.QUERY_NAME);
				query.setParameter(ReservationDataInternal.QGetToWithoutCancelled.P_TO, localTo.getMillis());
			}


		} else if (from != null && to == null) {

			DateTime localFrom = from.toDateTime(forTimeZone(localTimeZone));
			if (showCancelled) {

				query = em.get().createNamedQuery(ReservationDataInternal.QGetFrom.QUERY_NAME);
				query.setParameter(ReservationDataInternal.QGetFrom.P_FROM, localFrom.getMillis());

			} else {

				query = em.get().createNamedQuery(ReservationDataInternal.QGetFromWithoutCancelled.QUERY_NAME);
				query.setParameter(ReservationDataInternal.QGetFromWithoutCancelled.P_FROM, localFrom.getMillis());
			}

		} else {

			DateTime localFrom = from.toDateTime(forTimeZone(localTimeZone));
			DateTime localTo = to.toDateTime(forTimeZone(localTimeZone));

			if (showCancelled) {

				query = em.get().createNamedQuery(ReservationDataInternal.QGetByInterval.QUERY_NAME);
				query.setParameter(ReservationDataInternal.QGetByInterval.P_FROM, localFrom.getMillis());
				query.setParameter(ReservationDataInternal.QGetByInterval.P_TO, localTo.getMillis());

			} else {

				query = em.get().createNamedQuery(ReservationDataInternal.QGetByIntervalWithoutCancelled.QUERY_NAME);
				query.setParameter(ReservationDataInternal.QGetByIntervalWithoutCancelled.P_FROM, localFrom.getMillis()
				);
				query.setParameter(ReservationDataInternal.QGetByIntervalWithoutCancelled.P_TO, localTo.getMillis());
			}
		}

		if (offset != null) {
			query.setFirstResult(offset);
		}

		if (amount != null) {
			query.setMaxResults(amount);
		}

		try {

			return convertConfidentialReservationData(
					(List<ReservationDataInternal>) query.getResultList(),
					this.localTimeZone
			);

		} catch (DatatypeConfigurationException e) {
			throw new RSFault_Exception(e.getMessage(), new RSFault());
		}
	}

    @Override
	@Transactional
    public List<ConfidentialReservationData> getNonFinalizedReservations() throws RSFault_Exception {
        @SuppressWarnings("unchecked")
        final List<ReservationDataInternal> resultList = (List<ReservationDataInternal>) em.get()
                .createNamedQuery(ReservationDataInternal.QGetNonFinalized.QUERY_NAME).getResultList();

        try {
            return convertConfidentialReservationData(resultList, localTimeZone);
        } catch (DatatypeConfigurationException e) {
            throw new RSFault_Exception(e.getMessage(), new RSFault());
        }
    }

    @Override
	@Transactional
	public List<ConfidentialReservationData> getActiveReservations() throws RSFault_Exception {

		@SuppressWarnings("unchecked")
		final List<ReservationDataInternal> resultList = (List<ReservationDataInternal>) em.get()
				.createNamedQuery(ReservationDataInternal.QGetActive.QUERY_NAME)
				.setParameter(ReservationDataInternal.QGetActive.P_NOW, DateTime.now().getMillis())
				.getResultList();

		try {
			return convertConfidentialReservationData(resultList, localTimeZone);
		} catch (DatatypeConfigurationException e) {
			throw new RSFault_Exception(e.getMessage(), new RSFault());
		}
	}

	@Override
	@Transactional
	public List<ConfidentialReservationData> getFutureReservations() throws RSFault_Exception {

		@SuppressWarnings("unchecked")
		final List<ReservationDataInternal> resultList = (List<ReservationDataInternal>) em.get()
				.createNamedQuery(ReservationDataInternal.QGetFuture.QUERY_NAME)
				.setParameter(ReservationDataInternal.QGetFuture.P_NOW, DateTime.now().getMillis())
				.getResultList();

		try {
			return convertConfidentialReservationData(resultList, localTimeZone);
		} catch (DatatypeConfigurationException e) {
			throw new RSFault_Exception(e.getMessage(), new RSFault());
		}
	}

	@Override
	@Transactional
	public List<ConfidentialReservationData> getActiveAndFutureReservations() throws RSFault_Exception {

		@SuppressWarnings("unchecked")
		final List<ReservationDataInternal> resultList = (List<ReservationDataInternal>) em.get()
				.createNamedQuery(ReservationDataInternal.QGetActiveAndFuture.QUERY_NAME)
				.setParameter(ReservationDataInternal.QGetActiveAndFuture.P_NOW, DateTime.now().getMillis())
				.getResultList();

		try {
			return convertConfidentialReservationData(resultList, localTimeZone);
		} catch (DatatypeConfigurationException e) {
			throw new RSFault_Exception(e.getMessage(), new RSFault());
		}
	}

	@Override
	@Transactional
	public Optional<ConfidentialReservationData> getReservation(final NodeUrn nodeUrn, final DateTime timestamp)
			throws RSFault_Exception {

		@SuppressWarnings("unchecked")
		final List<ReservationDataInternal> resultList = (List<ReservationDataInternal>) em.get()
				.createNamedQuery(ReservationDataInternal.QGetByNodeAndTime.QUERY_NAME)
				.setParameter(ReservationDataInternal.QGetByNodeAndTime.P_TIMESTAMP, timestamp.getMillis())
				.setParameter(ReservationDataInternal.QGetByNodeAndTime.P_NODE_URN, nodeUrn.toString())
				.getResultList();

		if (resultList.isEmpty()) {
			return Optional.empty();
		}

		if (resultList.size() > 1) {
			throw new RSFault_Exception(
					"Found more than one reservation for node URN " + nodeUrn + " at timestamp " + timestamp,
					new RSFault()
			);
		}

		try {
			return Optional.of(TypeConverter.convertConfidentialReservationData(resultList, localTimeZone).get(0));
		} catch (DatatypeConfigurationException e) {
			throw new RSFault_Exception(e.getMessage(), new RSFault());
		}
	}
}
