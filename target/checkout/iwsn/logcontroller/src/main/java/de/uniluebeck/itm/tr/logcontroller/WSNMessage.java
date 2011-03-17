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

package de.uniluebeck.itm.tr.logcontroller;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import eu.wisebed.testbed.api.wsn.v22.Message;

import javax.persistence.*;


@Entity
@Table(name = "WsnMessages")
public class WSNMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String reservationKey;

	private String timeStamp;

	private String sourceNodeId;

	@Lob
	private byte[] binaryData;

	/**
	 * Converts XML message type into JPA entity
	 *
	 * @param message XML message
	 *
	 * @return JPA entity
	 */
	public static WSNMessage convertFromXMLMessage(Message message) {
		WSNMessage result = new WSNMessage();
		result.sourceNodeId = message.getSourceNodeId();
		result.timeStamp = message.getTimestamp().toString();
		result.binaryData = message.getBinaryData();
		return result;
	}

	/**
	 * Converts a JPA entity into XML message type
	 *
	 * @param from the JPA entity
	 *
	 * @return the XML message type
	 */
	public static Message convertToXMLMessage(WSNMessage from) {
		Message mes = new Message();
		mes.setTimestamp(XMLGregorianCalendarImpl.parse(from.timeStamp));
		mes.setSourceNodeId(from.sourceNodeId);
		mes.setBinaryData(from.binaryData);
		return mes;
	}

	public byte[] getBinaryData() {
		return binaryData;
	}

	public void setBinaryData(final byte[] binaryData) {
		this.binaryData = binaryData;
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getSourceNodeId() {
		return sourceNodeId;
	}

	public void setSourceNodeId(final String sourceNodeId) {
		this.sourceNodeId = sourceNodeId;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(final String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public void setReservationKey(final String reservationKey) {
		this.reservationKey = reservationKey;
	}
}
