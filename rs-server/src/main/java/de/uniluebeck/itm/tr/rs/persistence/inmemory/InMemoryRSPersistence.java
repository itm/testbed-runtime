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
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static eu.wisebed.api.v3.WisebedServiceHelper.createRSUnknownSecretReservationKeyFault;

public class InMemoryRSPersistence implements RSPersistence {

	private static final Logger log = LoggerFactory.getLogger(InMemoryRSPersistence.class);

	private SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1,
			new ThreadFactoryBuilder().setNameFormat("InMemoryRSPersistence-Thread %d").build()
	);

	private HashMap<SecretReservationKey, ConfidentialReservationData> reservations =
			new HashMap<SecretReservationKey, ConfidentialReservationData>();

	private Runnable housekeeper = new Runnable() {

		@Override
		public void run() {
			//log.debug("Doing housekeeping. Checking for invalidated sessions (current " + reservations.size() + ")");

			for (Iterator<Map.Entry<SecretReservationKey, ConfidentialReservationData>> iterator =
						 reservations.entrySet().iterator(); iterator.hasNext(); ) {
				Map.Entry<SecretReservationKey, ConfidentialReservationData> entry = iterator.next();

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
	public ConfidentialReservationData addReservation(final List<NodeUrn> nodeUrns,
													  final DateTime from,
													  final DateTime to,
													  final String username,
													  final NodeUrnPrefix urnPrefix,
													  final String description,
													  final List<KeyValuePair> options) throws RSFault_Exception {

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(from);
		crd.setTo(to);
		crd.getNodeUrns().addAll(nodeUrns);
		crd.setDescription(description);
		crd.getOptions().addAll(options);
		crd.setUsername(username);

		final SecretReservationKey secretReservationKey = new SecretReservationKey();
		secretReservationKey.setUrnPrefix(urnPrefix);
		secretReservationKey.setKey(secureIdGenerator.getNextId());

		crd.setSecretReservationKey(secretReservationKey);

		// remember in the HashMap (aka In-Memory-Storage)
		reservations.put(secretReservationKey, crd);

		return crd;
	}

	@Override
	public List<ConfidentialReservationData> getReservations(Interval interval) {

		List<ConfidentialReservationData> res = new LinkedList<ConfidentialReservationData>();

		for (ConfidentialReservationData r : reservations.values()) {
			Interval reserved = new Interval(
					new DateTime(r.getFrom().toGregorianCalendar()),
					new DateTime(r.getTo().toGregorianCalendar())
			);
			if (reserved.overlaps(interval)) {
				res.add(r);
			}

		}

		return res;
	}

	@Override
	public ConfidentialReservationData getReservation(SecretReservationKey secretReservationKey) throws
			UnknownSecretReservationKeyFault {
		ConfidentialReservationData confidentialReservationData = reservations.get(secretReservationKey);
		if (confidentialReservationData != null) {
			return confidentialReservationData;
		} else {
			throw createRSUnknownSecretReservationKeyFault("Reservation not found", secretReservationKey);
		}
	}

	@Override
	public ConfidentialReservationData deleteReservation(SecretReservationKey secretReservationKey) throws
			UnknownSecretReservationKeyFault {
		ConfidentialReservationData confidentialReservationData = reservations.remove(secretReservationKey);
		if (confidentialReservationData != null) {
			return confidentialReservationData;
		} else {
			throw createRSUnknownSecretReservationKeyFault("Reservation not found", secretReservationKey);
		}
	}

	@Override
	public String toString() {
		return "InMemoryRSPersistence{}";
	}

}
