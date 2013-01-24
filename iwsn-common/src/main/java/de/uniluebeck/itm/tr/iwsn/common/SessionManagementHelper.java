/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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


package de.uniluebeck.itm.tr.iwsn.common;

import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.sm.ExperimentNotRunningFault;
import eu.wisebed.api.v3.sm.ExperimentNotRunningFault_Exception;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class SessionManagementHelper {

	/**
	 * Calculates an instance hash based on the set of (secretReservationKey,urnPrefix)-tuples that are provided in {@code
	 * secretReservationKeys}.
	 *
	 * @param secretReservationKeys
	 * 		the list of {@link eu.wisebed.api.v3.common.SecretReservationKey} instances that
	 * 		contain the (secretReservationKey,urnPrefix)-tuples used for the calculation
	 *
	 * @return an instance hash
	 */
	public static String calculateWSNInstanceHash(List<SecretReservationKey> secretReservationKeys) {
		// secretReservationKey -> urnPrefix
		Map<String, NodeUrnPrefix> map = new TreeMap<String, NodeUrnPrefix>();
		for (SecretReservationKey secretReservationKey : secretReservationKeys) {
			map.put(secretReservationKey.getSecretReservationKey(), secretReservationKey.getUrnPrefix());
		}
		return "wsnInstanceHash" + map.hashCode();
	}

	public static ExperimentNotRunningFault_Exception createExperimentNotRunningException(
			final String secretReservationKey) {

		String msg = "Experiment with secret reservation key \"" + secretReservationKey + "\" either does not exist "
				+ "or is currently not running.";

		ExperimentNotRunningFault exception = new ExperimentNotRunningFault();
		exception.setMessage(msg);
		return new ExperimentNotRunningFault_Exception(msg, exception);
	}
}