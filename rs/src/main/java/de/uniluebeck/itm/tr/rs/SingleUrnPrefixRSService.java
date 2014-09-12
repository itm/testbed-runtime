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

package de.uniluebeck.itm.tr.rs;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.Constants;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import org.joda.time.DateTime;

import javax.jws.WebService;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the interface defines the reservation system (RS) for the WISEBED
 * experimental facilities.<br/>
 * This implementation is accessible via web services.
 */
@WebService(
		name = "RS",
		endpointInterface = "eu.wisebed.api.v3.rs.RS",
		portName = "RSPort",
		serviceName = "RSService",
		targetNamespace = "http://wisebed.eu/api/v3/rs"
)
public class SingleUrnPrefixRSService extends AbstractService implements de.uniluebeck.itm.tr.rs.RSService {

	/**
	 * Testbed runtime internal implementation of the reservation system's functionality.
	 * This implementation is not necessarily bound to web services or any other RPC technology.
	 */
	private final eu.wisebed.api.v3.rs.RS reservationSystem;

	private final RSServiceConfig rsServiceConfig;

	private final ServicePublisher servicePublisher;

	private ServicePublisherService jaxWsService;

	@Inject
	public SingleUrnPrefixRSService(final ServicePublisher servicePublisher, final RS rs,
									final RSServiceConfig rsServiceConfig) {
		this.servicePublisher = checkNotNull(servicePublisher);
		this.reservationSystem = checkNotNull(rs);
		this.rsServiceConfig = checkNotNull(rsServiceConfig);
	}

	@Override
	public List<ConfidentialReservationData> getReservation(
			final List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {

		return reservationSystem.getReservation(secretReservationKeys);
	}

	@Override
	public void deleteReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
								  final List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault, AuthorizationFault, AuthenticationFault {

		reservationSystem.deleteReservation(secretAuthenticationKeys, secretReservationKeys);
	}

	@Override
	public List<SecretReservationKey> makeReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
													  final List<NodeUrn> nodeUrns,
													  final DateTime from,
													  final DateTime to,
													  final String description,
													  final List<KeyValuePair> options)
			throws AuthorizationFault, RSFault_Exception, ReservationConflictFault_Exception, AuthenticationFault {

		return reservationSystem.makeReservation(secretAuthenticationKeys, nodeUrns, from, to, description, options);
	}

	@Override
	public List<PublicReservationData> getReservations(final DateTime from, final DateTime to,
													   final Integer offset, final Integer amount,
													   final Boolean showCancelled)
			throws RSFault_Exception {

		return reservationSystem.getReservations(from, to, offset, amount, showCancelled);
	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			final List<SecretAuthenticationKey> secretAuthenticationKey,
			final DateTime from, final DateTime to,
			final Integer offset, final Integer amount,
			final Boolean showCancelled)
			throws RSFault_Exception, AuthorizationFault, AuthenticationFault {

		return reservationSystem.getConfidentialReservations(
				secretAuthenticationKey,
				from,
				to,
				offset,
				amount,
				showCancelled
		);
	}


	@Override
	protected void doStart() {
		try {
			jaxWsService = servicePublisher.createJaxWsService(Constants.SOAP_API_V3.RS_CONTEXT_PATH, this, null);
			jaxWsService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (jaxWsService != null) {
				jaxWsService.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
