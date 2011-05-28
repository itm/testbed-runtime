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

package eu.wisebed.testbed.api.wsn;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uniluebeck.itm.tr.util.NetworkUtils;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.testbed.api.wsn.deliverymanager.DeliveryManager;

public class SessionManagementPreconditions {

	private CommonPreconditions commonPreconditions;

	public SessionManagementPreconditions() {
		this.commonPreconditions = new CommonPreconditions();
	}

	public void addKnownNodeUrns(final String... knownNodeUrns) {
		commonPreconditions.addKnownNodeUrns(knownNodeUrns);
	}

	public void addServedUrnPrefixes(String... servedUrnPrefixes) {
		commonPreconditions.addServedUrnPrefixes(servedUrnPrefixes);
	}

	public void checkGetInstanceArguments(List<SecretReservationKey> secretReservationKey, String controller) {
		checkGetInstanceArguments(secretReservationKey, controller, false);
	}

	public void checkGetInstanceArguments(List<SecretReservationKey> secretReservationKey, String controller,
										  boolean singleUrnImplementation) {

		checkNotNull(secretReservationKey);
		checkNotNull(controller);

		checkUrnPrefixesServed(secretReservationKey);

		if (singleUrnImplementation) {
			checkArgument(secretReservationKey.size() == 1,
					"There must be exactly one secret reservation key as this is a single URN-prefix implementation."
			);
		}
	}

	public void checkFreeArguments(List<SecretReservationKey> secretReservationKeyList) {

		checkNotNull(secretReservationKeyList);
	}

	public void checkAreNodesAliveArguments(final Collection<String> nodes, final String controllerEndpointUrl) {
		commonPreconditions.checkNodesKnown(nodes);
		NetworkUtils.checkConnectivity(controllerEndpointUrl);
	}

	private void checkUrnPrefixesServed(List<SecretReservationKey> secretReservationKeys) {

		// extract URN prefixes from secretReservationKey list and check if they're served by this instance
		Set<String> urnPrefixes = new HashSet<String>();
		for (SecretReservationKey key : secretReservationKeys) {

			checkNotNull(key.getUrnPrefix());
			checkNotNull(key.getSecretReservationKey());

			urnPrefixes.add(key.getUrnPrefix());

		}

		commonPreconditions.checkUrnPrefixesServed(urnPrefixes);
	}
}
