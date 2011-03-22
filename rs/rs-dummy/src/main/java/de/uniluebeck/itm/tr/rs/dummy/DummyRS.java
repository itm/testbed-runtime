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

package de.uniluebeck.itm.tr.rs.dummy;

import eu.wisebed.testbed.api.rs.v1.*;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@WebService(endpointInterface = "eu.wisebed.testbed.api.rs.v1.RS", portName = "RSPort", serviceName = "RSService", targetNamespace = "urn:RSService")
public class DummyRS implements RS {

	private Random r = new SecureRandom();

	@Override
	public List<PublicReservationData> getReservations(@WebParam(name = "from", targetNamespace = "") XMLGregorianCalendar from,
													   @WebParam(name = "to", targetNamespace = "") XMLGregorianCalendar to) throws RSExceptionException {

		return Collections.emptyList();

	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			@WebParam(name = "secretAuthenticationKey", targetNamespace = "")
			List<SecretAuthenticationKey> secretAuthenticationKey,
			@WebParam(name = "period", targetNamespace = "") GetReservations period) throws RSExceptionException {

		return Collections.emptyList();

	}

	@Override
	public List<ConfidentialReservationData> getReservation(
			@WebParam(name = "secretReservationKey", targetNamespace = "") List<SecretReservationKey> secretReservationKey)
			throws RSExceptionException, ReservervationNotFoundExceptionException {

		String msg = "Reservation not found (not implemented, this is the dummy implementation";
		RSException exception = new RSException();
		exception.setMessage(msg);
		throw new RSExceptionException(msg, exception);

	}

	@Override
	public void deleteReservation(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKey)
			throws RSExceptionException, ReservervationNotFoundExceptionException {

		// nothing to do as this is a dummy
	}

	@Override
	public List<SecretReservationKey> makeReservation(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "reservation", targetNamespace = "") ConfidentialReservationData reservation)
			throws AuthorizationExceptionException, RSExceptionException, ReservervationConflictExceptionException {

		List<SecretReservationKey> keys = new ArrayList<SecretReservationKey>(authenticationData.size());

		for (SecretAuthenticationKey key : authenticationData) {
			SecretReservationKey secretAuthenticationKey = new SecretReservationKey();
			secretAuthenticationKey.setUrnPrefix(key.getUrnPrefix());
			secretAuthenticationKey.setSecretReservationKey(Long.toString(r.nextLong()));
			keys.add(secretAuthenticationKey);
		}

		return keys;

	}

}
