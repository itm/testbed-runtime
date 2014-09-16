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

package de.uniluebeck.itm.tr.rs.persistence;

import com.google.common.base.Optional;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.List;

public interface RSPersistence {

	ConfidentialReservationData addReservation(List<NodeUrn> nodeUrns,
											   DateTime from,
											   DateTime to,
											   String username,
											   NodeUrnPrefix urnPrefix,
											   String description,
											   List<KeyValuePair> options) throws RSFault_Exception;

	List<ConfidentialReservationData> getReservations(
			@Nullable final DateTime from,
			@Nullable final DateTime to,
			@Nullable final Integer offset,
			@Nullable final Integer amount,
			@Nullable final Boolean showCancelled)
			throws RSFault_Exception;

	List<ConfidentialReservationData> getActiveReservations() throws RSFault_Exception;

	List<ConfidentialReservationData> getFutureReservations() throws RSFault_Exception;

	List<ConfidentialReservationData> getActiveAndFutureReservations() throws RSFault_Exception;

	/**
	 * Returns the reservation data for a reservation containing {@code nodeUrn} that is/was/will be active at {@code
	 * timestamp}.
	 *
	 * @param nodeUrn the nodeUrn that is part of the reservation
	 * @param timestamp the point in time in which the reservation shall be active
	 *
	 * @return a reservation data if found or none if not
	 */
	Optional<ConfidentialReservationData> getReservation(NodeUrn nodeUrn, DateTime timestamp) throws RSFault_Exception;

	ConfidentialReservationData getReservation(SecretReservationKey secretReservationKey)
			throws UnknownSecretReservationKeyFault, RSFault_Exception;

	ConfidentialReservationData cancelReservation(SecretReservationKey secretReservationKey)
			throws UnknownSecretReservationKeyFault, RSFault_Exception;

	void addListener(RSPersistenceListener rsPersistenceListener);

	void removeListener(RSPersistenceListener rsPersistenceListener);
}
