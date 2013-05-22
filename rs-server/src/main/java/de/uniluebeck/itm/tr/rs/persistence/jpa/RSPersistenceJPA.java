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

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.ReservationDataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.SecretReservationKeyInternal;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;
import static eu.wisebed.api.v3.WisebedServiceHelper.createRSUnknownSecretReservationKeyFault;

public class RSPersistenceJPA implements RSPersistence {

	private static final Logger log = LoggerFactory.getLogger(RSPersistence.class);

	private final EntityManager em;

	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private final TimeZone localTimeZone;

	@Inject
	public RSPersistenceJPA(final EntityManager em, final TimeZone timeZone) {
		this.em = checkNotNull(em);
		this.localTimeZone = checkNotNull(timeZone);
	}

	@Override
	public ConfidentialReservationData addReservation(final List<NodeUrn> nodeUrns,
													  final DateTime from,
													  final DateTime to,
													  final String username,
													  final NodeUrnPrefix urnPrefix,
													  final String description,
													  final List<KeyValuePair> options) throws RSFault_Exception {

		SecretReservationKeyInternal secretReservationKeyInternal = null;
		String generatedSecretReservationKey = null;

		boolean created = false;
		while (!created) {

			generatedSecretReservationKey = secureIdGenerator.getNextId();

			secretReservationKeyInternal = new SecretReservationKeyInternal();
			secretReservationKeyInternal.setSecretReservationKey(generatedSecretReservationKey);
			secretReservationKeyInternal.setUrnPrefix(urnPrefix.toString());

			try {

				em.getTransaction().begin();
				em.persist(secretReservationKeyInternal);
				em.getTransaction().commit();

				created = true;

			} catch (RollbackException e) {

				em.clear();

			} catch (Exception e) {
				log.error("Could not add SecretReservationKeyInternal because of: {}", e.getMessage());
				return null;
			}
		}

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

		try {

			em.getTransaction().begin();
			em.persist(reservationData);
			em.getTransaction().commit();

			return TypeConverter.convert(reservationData.getConfidentialReservationData(), localTimeZone);

		} catch (Exception e) {

			em.getTransaction().begin();
			em.remove(secretReservationKeyInternal);
			em.getTransaction().commit();

			String msg = "Could not add Reservation because of: " + e.getMessage();
			log.error(msg);
			RSFault exception = new RSFault();
			exception.setMessage(msg);
			throw new RSFault_Exception(msg, exception, e);

		}
	}

	@Override
	public ConfidentialReservationData getReservation(SecretReservationKey srk)
			throws UnknownSecretReservationKeyFault, RSFault_Exception {
		Query query = em.createNamedQuery(ReservationDataInternal.QGetByReservationKey.QUERY_NAME);
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
	public ConfidentialReservationData deleteReservation(SecretReservationKey srk)
			throws UnknownSecretReservationKeyFault, RSFault_Exception {
		Query query = em.createNamedQuery(ReservationDataInternal.QGetByReservationKey.QUERY_NAME);
		query.setParameter(ReservationDataInternal.QGetByReservationKey.P_SECRET_RESERVATION_KEY, srk.getKey());
		ReservationDataInternal reservationData;
		try {
			reservationData = (ReservationDataInternal) query.getSingleResult();
		} catch (NoResultException e) {
			throw createRSUnknownSecretReservationKeyFault("Reservation not found", srk);
		}
		reservationData.delete();
		em.getTransaction().begin();
		em.persist(reservationData);
		em.getTransaction().commit();

		try {
			return TypeConverter.convert(reservationData.getConfidentialReservationData(), this.localTimeZone);
		} catch (DatatypeConfigurationException e) {
			throw new RSFault_Exception(e.getMessage(), new RSFault());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ConfidentialReservationData> getReservations(Interval interval) throws RSFault_Exception {

		// transforming to default timezone
		GregorianCalendar from = interval.getStart().toGregorianCalendar();
		from.setTimeZone(this.localTimeZone);
		GregorianCalendar to = interval.getEnd().toGregorianCalendar();
		from.setTimeZone(this.localTimeZone);

		Query query = em.createNamedQuery(ReservationDataInternal.QGetByInterval.QUERY_NAME);
		query.setParameter(ReservationDataInternal.QGetByInterval.P_FROM, from.getTimeInMillis());
		query.setParameter(ReservationDataInternal.QGetByInterval.P_TO, to.getTimeInMillis());

		try {
			return TypeConverter.convertConfidentialReservationData((List<ReservationDataInternal>) query
					.getResultList(), this.localTimeZone
			);
		} catch (DatatypeConfigurationException e) {
			throw new RSFault_Exception(e.getMessage(), new RSFault());
		}
	}
}
