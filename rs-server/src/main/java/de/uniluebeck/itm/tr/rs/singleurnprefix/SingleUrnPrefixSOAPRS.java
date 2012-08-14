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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.rs.singleurnprefix;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.rs.NonWS;
import eu.wisebed.api.common.SecretAuthenticationKey;
import eu.wisebed.api.common.SecretReservationKey;
import eu.wisebed.api.rs.*;

import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * Implementation of the interface defines the reservation system (RS) for the WISEBED
 * experimental facilities.<br/>
 * This implementation is accessible via web services.
 */
@WebService(
		endpointInterface = "eu.wisebed.api.rs.RS",
		portName = "RSPort",
		serviceName = "RSService",
		targetNamespace = "urn:RSService"
)
public class SingleUrnPrefixSOAPRS implements RS {

	/**
	 * Testbed runtime internal implementation of the reservation system's functionality.
	 * This implementation is not necessarily bound to web services or any other RPC technology.
	 */
	@Inject
	@NonWS
	private RS reservationSystem;

	@Override
	public List<SecretReservationKey> makeReservation(final List<SecretAuthenticationKey> authenticationData,
													  final ConfidentialReservationData reservation)
			throws AuthorizationExceptionException, ReservationConflictExceptionException, RSExceptionException {

		return reservationSystem.makeReservation(authenticationData, reservation);

	}

	@Override
	public List<ConfidentialReservationData> getReservation(
			final List<SecretReservationKey> secretReservationKeys)
			throws RSExceptionException, ReservationNotFoundExceptionException {

		return reservationSystem.getReservation(secretReservationKeys);
	}

	@Override
	public void deleteReservation(final List<SecretReservationKey> secretReservationKeys)
			throws RSExceptionException, ReservationNotFoundExceptionException {

		reservationSystem.deleteReservation(secretReservationKeys);
	}

	@Override
	public List<PublicReservationData> getReservations(final XMLGregorianCalendar from, final XMLGregorianCalendar to)
			throws RSExceptionException {

		return reservationSystem.getReservations(from, to);
	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			final List<SecretAuthenticationKey> secretAuthenticationKeys,
			final GetReservations period) throws RSExceptionException {

		return reservationSystem.getConfidentialReservations(secretAuthenticationKeys, period);
	}

}
