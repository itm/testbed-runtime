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

import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;

import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@WebService(
		name = "RS",
		endpointInterface = "eu.wisebed.api.v3.rs.RS",
		portName = "RSPort",
		serviceName = "RSService",
		targetNamespace = "http://wisebed.eu/api/v3/rs"
)
public class DummyRS implements RS {

	private Random r = new SecureRandom();

	@Override
	public List<PublicReservationData> getReservations(final XMLGregorianCalendar from,
													   final XMLGregorianCalendar to) throws RSFault_Exception {
		return Collections.emptyList();

	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			final List<SecretAuthenticationKey> secretAuthenticationKey,
			final XMLGregorianCalendar from,
			final XMLGregorianCalendar to) throws RSFault_Exception {

		return Collections.emptyList();
	}

	@Override
	public List<ConfidentialReservationData> getReservation(final List<SecretReservationKey> secretReservationKey)
			throws RSFault_Exception, ReservationNotFoundFault_Exception {

		String msg = "Reservation not found (not implemented, this is the dummy implementation";
		RSFault exception = new RSFault();
		exception.setMessage(msg);
		throw new RSFault_Exception(msg, exception);

	}

	@Override
	public void deleteReservation(final List<SecretReservationKey> secretReservationKey)
			throws RSFault_Exception, ReservationNotFoundFault_Exception {

		// nothing to do as this is a dummy
	}

	@Override
	public List<SecretReservationKey> makeReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
													  final List<String> nodeUrns,
													  final XMLGregorianCalendar from,
													  final XMLGregorianCalendar to)
			throws AuthorizationFault_Exception, RSFault_Exception, ReservationConflictFault_Exception {

		List<SecretReservationKey> keys = new ArrayList<SecretReservationKey>(secretAuthenticationKeys.size());

		for (SecretAuthenticationKey key : secretAuthenticationKeys) {
			SecretReservationKey secretAuthenticationKey = new SecretReservationKey();
			secretAuthenticationKey.setUrnPrefix(key.getUrnPrefix());
			secretAuthenticationKey.setSecretReservationKey(Long.toString(r.nextLong()));
			keys.add(secretAuthenticationKey);
		}

		return keys;
	}
}
