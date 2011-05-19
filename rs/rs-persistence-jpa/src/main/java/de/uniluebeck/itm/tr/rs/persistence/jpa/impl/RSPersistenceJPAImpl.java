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

package de.uniluebeck.itm.tr.rs.persistence.jpa.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.ReservationDataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.SecretReservationKeyInternal;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.api.rs.*;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.xml.datatype.DatatypeConfigurationException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class RSPersistenceJPAImpl implements RSPersistence {

	private static final Logger logger = LoggerFactory.getLogger(RSPersistence.class);

	private EntityManager manager;

	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();
	private TimeZone localTimeZone;

	@Inject
	public RSPersistenceJPAImpl(@Named("properties") Map properties, TimeZone localTimeZone) {
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("default", properties);
		//EntityManagerFactory factory = Persistence.createEntityManagerFactory("default");
		manager = factory.createEntityManager();
		this.localTimeZone = localTimeZone;
	}

	/**
	 * old
	 *
	 * @Inject public RSPersistenceJPAImpl(@Named("persistenceUnitName") String persistenceUnitName) {
	 * EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName);
	 * manager = factory.createEntityManager();
	 * }*
	 */

	public SecretReservationKey addReservation(ConfidentialReservationData confidentialReservationData, String urnPrefix)
			throws RSExceptionException {

		SecretReservationKeyInternal secretReservationKey = null;

		// save SecretReservationKeyInternal
		boolean created = false;
		while (!created) {

			secretReservationKey = new SecretReservationKeyInternal();
			secretReservationKey.setSecretReservationKey(secureIdGenerator.getNextId());
			secretReservationKey.setUrnPrefix(urnPrefix);

			try {

				manager.getTransaction().begin();
				manager.persist(secretReservationKey);
				manager.getTransaction().commit();

				created = true;

			} catch (RollbackException e) {

				manager.clear();

			} catch (Exception e) {
				logger.error("Could not add SecretReservationKeyInternal because of: {}", e.getMessage());
				e.printStackTrace();
				return null;
			}
		}

		ReservationDataInternal reservationData = new ReservationDataInternal(secretReservationKey, TypeConverter
				.convert(confidentialReservationData, localTimeZone), urnPrefix);
		try {

			// save ReservationDataInternal
			manager.getTransaction().begin();
			manager.persist(reservationData);
			manager.getTransaction().commit();

			return TypeConverter.convert(secretReservationKey);

		} catch (Exception e) {

			manager.getTransaction().begin();
			manager.remove(secretReservationKey);
			manager.getTransaction().commit();

			String msg = "Could not add Reservation because of: " + e.getMessage();
			logger.error(msg);
			RSException exception = new RSException();
			exception.setMessage(msg);
			throw new RSExceptionException(msg, exception, e);

		}
	}

	@Override
	public ConfidentialReservationData getReservation(SecretReservationKey secretReservationKey)
			throws ReservervationNotFoundExceptionException, RSExceptionException {
		Query query = manager.createNamedQuery(ReservationDataInternal.QGetByReservationKey.QUERYNAME);
		query.setParameter(ReservationDataInternal.QGetByReservationKey.P_SECRETRESERVATIONKEY, secretReservationKey
				.getSecretReservationKey());
		ReservationDataInternal reservationData = null;
		try {
			reservationData = (ReservationDataInternal) query.getSingleResult();
		} catch (NoResultException e) {
			throw new ReservervationNotFoundExceptionException(("Reservation " + secretReservationKey + " not found"),
					new ReservervationNotFoundException());
		}
		try {
			return TypeConverter.convert(reservationData.getConfidentialReservationData(), this.localTimeZone);
		} catch (DatatypeConfigurationException e) {
			throw new RSExceptionException(e.getMessage(), new RSException());
		}
	}

	@Override
	public ConfidentialReservationData deleteReservation(SecretReservationKey secretReservationKey)
			throws ReservervationNotFoundExceptionException, RSExceptionException {
		Query query = manager.createNamedQuery(ReservationDataInternal.QGetByReservationKey.QUERYNAME);
		query.setParameter(ReservationDataInternal.QGetByReservationKey.P_SECRETRESERVATIONKEY, secretReservationKey
				.getSecretReservationKey());
		ReservationDataInternal reservationData = null;
		try {
			reservationData = (ReservationDataInternal) query.getSingleResult();
		} catch (NoResultException e) {
			throw new ReservervationNotFoundExceptionException(("Reservation " + secretReservationKey + " not found"),
					new ReservervationNotFoundException());
		}
		reservationData.delete();
		manager.getTransaction().begin();
		manager.persist(reservationData);
		manager.getTransaction().commit();

		try {
			return TypeConverter.convert(reservationData.getConfidentialReservationData(), this.localTimeZone);
		} catch (DatatypeConfigurationException e) {
			throw new RSExceptionException(e.getMessage(), new RSException());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ConfidentialReservationData> getReservations(Interval interval) throws RSExceptionException {

		// transforming to default timezone
		GregorianCalendar from = interval.getStart().toGregorianCalendar();
		from.setTimeZone(this.localTimeZone);
		GregorianCalendar to = interval.getEnd().toGregorianCalendar();
		from.setTimeZone(this.localTimeZone);

		Query query = manager.createNamedQuery(ReservationDataInternal.QGetByInterval.QUERYNAME);
		query.setParameter(ReservationDataInternal.QGetByInterval.P_FROM, new Long(from.getTimeInMillis()));
		query.setParameter(ReservationDataInternal.QGetByInterval.P_TO, new Long(to.getTimeInMillis()));

		try {
			return TypeConverter.convertConfidentialReservationData((List<ReservationDataInternal>) query
					.getResultList(), this.localTimeZone);
		} catch (DatatypeConfigurationException e) {
			throw new RSExceptionException(e.getMessage(), new RSException());
		}
	}

	// temp

	public void printPersistentReservationData() throws Exception {
		System.out.println("ReservationTableEntries:\n----------");
		for (Object entry : manager.createQuery(
				"SELECT data FROM ReservationDataInternal data WHERE data.deleted = false").getResultList()) {
			ReservationDataInternal data = (ReservationDataInternal) entry;
			System.out.println(data.getId() + ", " + data.getSecretReservationKey() + ", "
					+ data.getConfidentialReservationData() + ", " + data.getUrnPrefix());
		}
	}
}
