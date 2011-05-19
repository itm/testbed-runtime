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

package de.uniluebeck.itm.tr.rs.persistence.jpa.impl;

import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.ConfidentialReservationDataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.DataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.ReservationDataInternal;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.SecretReservationKeyInternal;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.Data;
import eu.wisebed.api.rs.SecretReservationKey;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by IntelliJ IDEA.
 * User: bimschas
 * Date: 16.04.2010
 * Time: 19:19:46
 */
public class TypeConverter {

	public static SecretReservationKey convert(SecretReservationKeyInternal internal) {
		SecretReservationKey external = new SecretReservationKey();
		external.setSecretReservationKey(internal.getSecretReservationKey());
		external.setUrnPrefix(internal.getUrnPrefix());
		return external;
	}

	public static ConfidentialReservationDataInternal convert(ConfidentialReservationData external, TimeZone localTimeZone) {

		ConfidentialReservationDataInternal internal = new ConfidentialReservationDataInternal();

		GregorianCalendar fromGregorianCalendar = external.getFrom().toGregorianCalendar();

		// strange workaround to initialize object before setting timezone
		fromGregorianCalendar.getTimeInMillis();

		fromGregorianCalendar.setTimeZone(localTimeZone);
		internal.setFromDate(fromGregorianCalendar.getTimeInMillis());

		internal.setNodeURNs(external.getNodeURNs());
		internal.setData(convertExternalToInternal(external.getData()));

		GregorianCalendar toGregorianCalendar = external.getTo().toGregorianCalendar();

		// strange workaround to initialize object before setting timezone
		toGregorianCalendar.getTimeInMillis();

		toGregorianCalendar.setTimeZone(localTimeZone);
		internal.setToDate(toGregorianCalendar.getTimeInMillis());

		return internal;
	}

	private static List<DataInternal> convertExternalToInternal(List<Data> external) {
		List<DataInternal> internal = new ArrayList<DataInternal>(external.size());
		for (Data data : external) {
			internal.add(convert(data));
		}
		return internal;
	}

	private static DataInternal convert(Data external) {
		return new DataInternal(external.getUrnPrefix(), external.getUsername());
	}

	public static ConfidentialReservationData convert(ConfidentialReservationDataInternal internal, TimeZone localTimeZone) throws DatatypeConfigurationException {
		ConfidentialReservationData external = new ConfidentialReservationData();
		external.setFrom(convert(internal.getFromDate(), localTimeZone));
		external.getNodeURNs().addAll(internal.getNodeURNs());
		external.setTo(convert(internal.getToDate(), localTimeZone));
		external.getData().addAll(convertInternalToExternal(internal.getData()));
		return external;
	}

	private static List<Data> convertInternalToExternal(List<DataInternal> internalList) {
		List<Data> externalList = new ArrayList<Data>(internalList.size());
		for (DataInternal internal : internalList) {
			externalList.add(convert(internal));
		}
		return externalList;
	}

	private static Data convert(DataInternal internal) {
		Data external = new Data();
		external.setUrnPrefix(internal.getUrnPrefix());
		external.setUsername(internal.getUsername());
		return external;
	}

	private static XMLGregorianCalendar convert(long dateInMillis, TimeZone localTimeZone) throws DatatypeConfigurationException {
		GregorianCalendar fromGregorianCalendar = new GregorianCalendar(localTimeZone);
		fromGregorianCalendar.setTimeInMillis(dateInMillis);
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(fromGregorianCalendar);
	}

	public static List<ConfidentialReservationData> convertConfidentialReservationData(List<ReservationDataInternal> internalList, TimeZone localTimeZone) throws DatatypeConfigurationException {
		List<ConfidentialReservationData> externalList = new ArrayList<ConfidentialReservationData>(internalList.size());
		for (ReservationDataInternal internal : internalList) {
			externalList.add(convert(internal.getConfidentialReservationData(), localTimeZone));
		}
		return externalList;
	}

}
