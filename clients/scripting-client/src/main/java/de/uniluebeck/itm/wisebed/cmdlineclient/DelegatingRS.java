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

package de.uniluebeck.itm.wisebed.cmdlineclient;

import java.util.List;

import javax.jws.WebParam;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.wisebed.api.rs.AuthorizationExceptionException;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.GetReservations;
import eu.wisebed.api.rs.PublicReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.RSExceptionException;
import eu.wisebed.api.rs.ReservervationConflictExceptionException;
import eu.wisebed.api.rs.ReservervationNotFoundExceptionException;
import eu.wisebed.api.rs.SecretAuthenticationKey;
import eu.wisebed.api.rs.SecretReservationKey;

public class DelegatingRS implements RS {

	private RS delegate;

	private DelegatingRS(RS delegate) {
		this.delegate = delegate;
	}


	@Override
	public List<PublicReservationData> getReservations(
			@WebParam(name = "from", targetNamespace = "") XMLGregorianCalendar from,
			@WebParam(name = "to", targetNamespace = "") XMLGregorianCalendar to) throws RSExceptionException {
		return delegate.getReservations(from, to);
	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			@WebParam(name = "secretAuthenticationKey", targetNamespace = "")
			List<SecretAuthenticationKey> secretAuthenticationKey,
			@WebParam(name = "period", targetNamespace = "") GetReservations period) throws RSExceptionException {
		return delegate.getConfidentialReservations(secretAuthenticationKey, period);
	}

	@Override
	public List<ConfidentialReservationData> getReservation(
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKey)
			throws RSExceptionException, ReservervationNotFoundExceptionException {
		return delegate.getReservation(secretReservationKey);
	}

	@Override
	public void deleteReservation(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKey)
			throws RSExceptionException, ReservervationNotFoundExceptionException {
		delegate.deleteReservation(authenticationData, secretReservationKey);
	}

	@Override
	public List<SecretReservationKey> makeReservation(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "reservation", targetNamespace = "") ConfidentialReservationData reservation)
			throws AuthorizationExceptionException, RSExceptionException, ReservervationConflictExceptionException {
		return delegate.makeReservation(authenticationData, reservation);
	}
}
