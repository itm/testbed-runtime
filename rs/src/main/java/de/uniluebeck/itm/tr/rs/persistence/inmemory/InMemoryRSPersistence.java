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

import com.google.common.base.Optional;
import de.uniluebeck.itm.tr.rs.persistence.ConfidentialReservationDataComparator;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.util.SecureIdGenerator;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newLinkedList;
import static de.uniluebeck.itm.tr.rs.persistence.OffsetAmountHelper.limitResults;
import static eu.wisebed.api.v3.WisebedServiceHelper.createRSUnknownSecretReservationKeyFault;

public class InMemoryRSPersistence implements RSPersistence {

	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private final SortedSet<ConfidentialReservationData> reservations = new TreeSet<ConfidentialReservationData>(
			new ConfidentialReservationDataComparator()
	);

	@Override
	public synchronized ConfidentialReservationData addReservation(final List<NodeUrn> nodeUrns,
																   final DateTime from,
																   final DateTime to,
																   final String username,
																   final NodeUrnPrefix urnPrefix,
																   final String description,
																   final List<KeyValuePair> options)
			throws RSFault_Exception {

		final SecretReservationKey secretReservationKey = new SecretReservationKey();
		secretReservationKey.setUrnPrefix(urnPrefix);
		secretReservationKey.setKey(secureIdGenerator.getNextId());

		final ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(from);
		crd.setTo(to);
		crd.getNodeUrns().addAll(nodeUrns);
		crd.setDescription(description);
		crd.getOptions().addAll(options);
		crd.setUsername(username);
		crd.setSecretReservationKey(secretReservationKey);

		reservations.add(crd);

		return crd;
	}

	@Override
	public synchronized List<ConfidentialReservationData> getReservations(@Nullable final DateTime from,
																		  @Nullable final DateTime to,
																		  @Nullable Integer offset,
																		  @Nullable Integer amount) {

		checkArgument(offset == null || offset >= 0, "Parameter offset must be a non-negative integer or null");
		checkArgument(amount == null || amount >= 0, "Parameter amount must be a non-negative integer or null");

		final List<ConfidentialReservationData> matchingReservations = newLinkedList();

		for (ConfidentialReservationData reservation : reservations) {
			boolean match =
					(from == null && to == null) ||
					(to == null && (reservation.getFrom().equals(from) || reservation.getFrom().isBefore(from))) ||
					(from == null && (reservation.getTo().isBefore(to))) ||
					(from != null && to != null && new Interval(reservation.getFrom(), reservation.getTo()).overlaps(new Interval(from, to)));
			if (match) {
				matchingReservations.add(reservation);
			}
		}

		return limitResults(matchingReservations, offset, amount);
	}

	@Override
	public synchronized List<ConfidentialReservationData> getActiveReservations() throws RSFault_Exception {
		final List<ConfidentialReservationData> matchingReservations = newLinkedList();
		for (ConfidentialReservationData reservation : reservations) {
			if (new Interval(reservation.getFrom(), reservation.getTo()).containsNow()) {
				matchingReservations.add(reservation);
			}
		}
		return matchingReservations;
	}

	@Override
	public synchronized List<ConfidentialReservationData> getFutureReservations() throws RSFault_Exception {
		final List<ConfidentialReservationData> matchingReservations = newLinkedList();
		for (ConfidentialReservationData reservation : reservations) {
			if (new Interval(reservation.getFrom(), reservation.getTo()).isAfterNow()) {
				matchingReservations.add(reservation);
			}
		}
		return matchingReservations;
	}

	@Override
	public synchronized List<ConfidentialReservationData> getActiveAndFutureReservations() throws RSFault_Exception {
		final List<ConfidentialReservationData> matchingReservations = getActiveReservations();
		matchingReservations.addAll(getFutureReservations());
		return matchingReservations;
	}

	@Override
	public synchronized Optional<ConfidentialReservationData> getReservation(final NodeUrn nodeUrn, final DateTime timestamp)
			throws RSFault_Exception {

		for (ConfidentialReservationData reservation : reservations) {
			if (reservation.getNodeUrns().contains(nodeUrn) && new Interval(reservation.getFrom(), reservation.getTo())
					.contains(timestamp)) {
				return Optional.of(reservation);
			}
		}

		return Optional.absent();
	}

	@Override
	public synchronized ConfidentialReservationData getReservation(SecretReservationKey secretReservationKey) throws
			UnknownSecretReservationKeyFault {

		for (ConfidentialReservationData reservation : reservations) {
			if (reservation.getSecretReservationKey().equals(secretReservationKey)) {
				return reservation;
			}
		}

		throw createRSUnknownSecretReservationKeyFault("Reservation not found", secretReservationKey);
	}

	@Override
	public synchronized ConfidentialReservationData deleteReservation(SecretReservationKey secretReservationKey) throws
			UnknownSecretReservationKeyFault {

		for (Iterator<ConfidentialReservationData> iterator = reservations.iterator(); iterator.hasNext(); ) {
			ConfidentialReservationData crd = iterator.next();
			if (crd.getSecretReservationKey().equals(secretReservationKey)) {
				iterator.remove();
				return crd;
			}
		}

		throw createRSUnknownSecretReservationKeyFault("Reservation not found", secretReservationKey);
	}

	@Override
	public String toString() {
		return "InMemoryRSPersistence";
	}

}
