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

package de.uniluebeck.itm.tr.rs.persistence.jpa;

import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.ConfidentialReservationDataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.DataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.ReservationDataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.SecretReservationKeyInternal;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.ConfidentialReservationDataKey;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class TypeConverter {

	public static SecretReservationKey convert(SecretReservationKeyInternal internal) {
		SecretReservationKey external = new SecretReservationKey();
		external.setKey(internal.getSecretReservationKey());
		external.setUrnPrefix(new NodeUrnPrefix(internal.getUrnPrefix()));
		return external;
	}

	public static ConfidentialReservationDataInternal convert(ConfidentialReservationData external,
															  TimeZone localTimeZone) {

		ConfidentialReservationDataInternal internal = new ConfidentialReservationDataInternal();

		GregorianCalendar fromGregorianCalendar = external.getFrom().toGregorianCalendar();

		// strange workaround to initialize object before setting timezone
		fromGregorianCalendar.getTimeInMillis();

		fromGregorianCalendar.setTimeZone(localTimeZone);
		internal.setFromDate(fromGregorianCalendar.getTimeInMillis());

		List<String> nodeUrnStringList = newArrayList();
		for (NodeUrn nodeUrn : external.getNodeUrns()) {
			nodeUrnStringList.add(nodeUrn.toString());
		}
		internal.setNodeUrns(nodeUrnStringList);
		internal.setData(convertExternalToInternal(external.getKeys()));
		internal.setDescription(external.getDescription());
		Map<String, String> options = newHashMap();
		for (KeyValuePair keyValuePair : external.getOptions()) {
			options.put(keyValuePair.getKey(), keyValuePair.getValue());
		}
		internal.setOptions(options);

		GregorianCalendar toGregorianCalendar = external.getTo().toGregorianCalendar();

		// strange workaround to initialize object before setting timezone
		toGregorianCalendar.getTimeInMillis();

		toGregorianCalendar.setTimeZone(localTimeZone);
		internal.setToDate(toGregorianCalendar.getTimeInMillis());

		internal.setUserData("");

		return internal;
	}

	private static List<DataInternal> convertExternalToInternal(List<ConfidentialReservationDataKey> external) {
		List<DataInternal> internal = new ArrayList<DataInternal>(external.size());
		for (ConfidentialReservationDataKey key : external) {
			internal.add(convert(key));
		}
		return internal;
	}

	private static DataInternal convert(ConfidentialReservationDataKey external) {
		return new DataInternal(
				external.getUrnPrefix().toString(),
				external.getUsername(),
				external.getKey()
		);
	}

	public static ConfidentialReservationData convert(ConfidentialReservationDataInternal internal,
													  TimeZone localTimeZone) throws DatatypeConfigurationException {
		ConfidentialReservationData external = new ConfidentialReservationData();
		external.getKeys().addAll(convertInternalToExternal(internal.getData()));
		List<NodeUrn> nodeUrns = newArrayList();
		for (String nodeUrnString : internal.getNodeUrns()) {
			nodeUrns.add(new NodeUrn(nodeUrnString));
		}
		external.getNodeUrns().addAll(nodeUrns);
		final DateTime from = new DateTime(internal.getFromDate(), DateTimeZone.forTimeZone(localTimeZone));
		external.setFrom(from.toDateTime(DateTimeZone.getDefault()));
		final DateTime to = new DateTime(internal.getToDate(), DateTimeZone.forTimeZone(localTimeZone));
		external.setTo(to.toDateTime(DateTimeZone.getDefault()));
		external.setDescription(internal.getDescription());
		external.getOptions().addAll(convert(internal.getOptions()));
		return external;
	}

	private static List<KeyValuePair> convert(final Map<String, String> internal) {
		final List<KeyValuePair> external = newArrayList();
		for (Map.Entry<String, String> entry : internal.entrySet()) {
			final KeyValuePair pair = new KeyValuePair();
			pair.setKey(entry.getKey());
			pair.setValue(entry.getValue());
			external.add(pair);
		}
		return external;
	}

	private static List<ConfidentialReservationDataKey> convertInternalToExternal(List<DataInternal> internalList) {
		List<ConfidentialReservationDataKey> externalList =
				new ArrayList<ConfidentialReservationDataKey>(internalList.size());
		for (DataInternal internal : internalList) {
			externalList.add(convert(internal));
		}
		return externalList;
	}

	private static ConfidentialReservationDataKey convert(DataInternal internal) {
		ConfidentialReservationDataKey external = new ConfidentialReservationDataKey();
		external.setUrnPrefix(new NodeUrnPrefix(internal.getUrnPrefix()));
		external.setUsername(internal.getUsername());
		external.setKey(internal.getSecretReservationKey());
		return external;
	}

	public static List<ConfidentialReservationData> convertConfidentialReservationData(
			List<ReservationDataInternal> internalList, TimeZone localTimeZone) throws DatatypeConfigurationException {
		List<ConfidentialReservationData> externalList =
				new ArrayList<ConfidentialReservationData>(internalList.size());
		for (ReservationDataInternal internal : internalList) {
			externalList.add(convert(internal.getConfidentialReservationData(), localTimeZone));
		}
		return externalList;
	}

}
