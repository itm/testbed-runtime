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

package de.uniluebeck.itm.tr.rs.persistence.inmemory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.testbed.api.rs.v1.ConfidentialReservationData;
import eu.wisebed.testbed.api.rs.v1.ReservervationNotFoundExceptionException;
import eu.wisebed.testbed.api.rs.v1.ReservervationNotFoundException;
import eu.wisebed.testbed.api.rs.v1.SecretReservationKey;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by:
 * User: bimschas
 * Date: 11.03.2010
 * Time: 10:24:37
 */
public class InMemoryRSPersistence implements RSPersistence {

	private static final Logger log = LoggerFactory.getLogger(InMemoryRSPersistence.class);

	private SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("InMemoryRSPersistence-Thread %d").build());

	private HashMap<SecretReservationKeyWrapper, ConfidentialReservationData> reservations = new HashMap<SecretReservationKeyWrapper, ConfidentialReservationData>();

	private Runnable housekeeper = new Runnable() {

		@Override
		public void run() {
			//log.debug("Doing housekeeping. Checking for invalidated sessions (current " + reservations.size() + ")");

			for (Iterator<Map.Entry<SecretReservationKeyWrapper, ConfidentialReservationData>> iterator = reservations.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry<SecretReservationKeyWrapper, ConfidentialReservationData> entry = iterator.next();

				if (entry.getValue().getTo().toGregorianCalendar().getTimeInMillis() < System.currentTimeMillis()) {
					log.debug("Removing reservation during housekeeping: " + entry.getValue());
					iterator.remove();
				}
			}

			//log.debug("Housekeeping done, (current " + reservations.size() + ") sessions");
		}
	};

	public InMemoryRSPersistence() {
		timer.scheduleWithFixedDelay(housekeeper, 1, 1, TimeUnit.MINUTES);
	}

	@Override
	public SecretReservationKey addReservation(ConfidentialReservationData reservationData, String urnPrefix) {

		// construct the return object
		SecretReservationKey secretReservationKey = new SecretReservationKey();
		secretReservationKey.setUrnPrefix(urnPrefix);
		secretReservationKey.setSecretReservationKey(secureIdGenerator.getNextId());

		// wrap it so it can be used in a HashMap
		SecretReservationKeyWrapper secretReservationKeyWrapper = new SecretReservationKeyWrapper(secretReservationKey);

		// remember in the HashMap (aka In-Memory-Storage)
		reservations.put(secretReservationKeyWrapper, reservationData);

		return secretReservationKey;
		
	}

	@Override
	public List<ConfidentialReservationData> getReservations(Interval interval) {

		List<ConfidentialReservationData> res = new LinkedList<ConfidentialReservationData>();

		for (ConfidentialReservationData r : reservations.values()) {
			Interval reserved = new Interval(
					new DateTime(r.getFrom().toGregorianCalendar()),
					new DateTime(r.getTo().toGregorianCalendar())
			);
			if (reserved.overlaps(interval))
				res.add(r);

		}

		return res;
	}

	/**
	 * This class is used to wrap the JAX-WS-generated {@link SecretReservationKey} class
	 * with a {@link Object#equals(Object)} and a {@link Object#hashCode()} method so these
	 * are not lost if the JAX-WS class are newly generated. By wrapping the class it can
	 * be used e.g. inside a {@link java.util.HashMap}.
	 */
	private static class SecretReservationKeyWrapper {
		public SecretReservationKey secretReservationKey;

		private SecretReservationKeyWrapper(SecretReservationKey secretReservationKey) {
			this.secretReservationKey = secretReservationKey;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SecretReservationKeyWrapper other = (SecretReservationKeyWrapper) obj;
			if (secretReservationKey.getSecretReservationKey() == null) {
				if (other.secretReservationKey.getSecretReservationKey() != null)
					return false;
			} else if (!secretReservationKey.getSecretReservationKey().equals(other.secretReservationKey.getSecretReservationKey()))
				return false;
			if (secretReservationKey.getUrnPrefix() == null) {
				if (other.secretReservationKey.getUrnPrefix() != null)
					return false;
			} else if (!secretReservationKey.getUrnPrefix().equals(other.secretReservationKey.getUrnPrefix()))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((secretReservationKey.getSecretReservationKey() == null) ? 0 : secretReservationKey.getSecretReservationKey().hashCode());
			result = prime * result + ((secretReservationKey.getUrnPrefix() == null) ? 0 : secretReservationKey.getUrnPrefix().hashCode());
			return result;
		}
	}

	@Override
	public ConfidentialReservationData getReservation(SecretReservationKey secretReservationKey) throws
			ReservervationNotFoundExceptionException {
		SecretReservationKeyWrapper secretReservationKeyWrapper = new SecretReservationKeyWrapper(secretReservationKey);
		ConfidentialReservationData confidentialReservationData = reservations.get(secretReservationKeyWrapper);
		if (confidentialReservationData != null) {
			return confidentialReservationData;
		} else
			throw new ReservervationNotFoundExceptionException(("Reservation " + secretReservationKey + " not found"), new ReservervationNotFoundException());
	}

	@Override
	public ConfidentialReservationData deleteReservation(SecretReservationKey secretReservationKey) throws
			ReservervationNotFoundExceptionException {
		SecretReservationKeyWrapper secretReservationKeyWrapper = new SecretReservationKeyWrapper(secretReservationKey);
		ConfidentialReservationData confidentialReservationData = reservations.remove(secretReservationKeyWrapper);
		if (confidentialReservationData != null) {
			return confidentialReservationData;
		} else
			throw new ReservervationNotFoundExceptionException(("Reservation " + secretReservationKey + " not found"), new ReservervationNotFoundException());
	}

	@Override
	public String toString() {
		return "InMemoryRSPersistence{}";
	}

}
