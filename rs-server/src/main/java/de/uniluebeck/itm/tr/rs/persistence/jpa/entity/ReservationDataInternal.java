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

package de.uniluebeck.itm.tr.rs.persistence.jpa.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@NamedQueries({
		@NamedQuery(
				name = ReservationDataInternal.QGetByReservationKey.QUERYNAME,
				query = "FROM ReservationDataInternal data WHERE data.secretReservationKey.secretReservationKey = :" +
						ReservationDataInternal.QGetByReservationKey.P_SECRETRESERVATIONKEY +
						" AND data.deleted = false"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetByInterval.QUERYNAME,
				query = "FROM ReservationDataInternal data WHERE NOT (" +
						":" + ReservationDataInternal.QGetByInterval.P_TO + " <= data.confidentialReservationData.fromDate" +
						" OR " +
						":" + ReservationDataInternal.QGetByInterval.P_FROM + " >= data.confidentialReservationData.toDate" +
						")"

		)
})
public class ReservationDataInternal implements Serializable {

	public static class QGetByReservationKey {

		public static final String QUERYNAME = "getReservationDataBySecretReservationKey";

		public static final String P_SECRETRESERVATIONKEY = "secretReservationKey";
	}

	public static class QGetByInterval {

		public static final String QUERYNAME = "getReservationDataByInterval";

		public static final String P_FROM = "from";

		public static final String P_TO = "to";
	}

	@Id
	@GeneratedValue
	@Column(nullable = false)
	private long id;

	@OneToOne(cascade = javax.persistence.CascadeType.PERSIST)
	private ConfidentialReservationDataInternal confidentialReservationData;

	@OneToOne
	private SecretReservationKeyInternal secretReservationKey;

	@Column
	private String urnPrefix;

	@Column
	private boolean deleted = false;

	public ReservationDataInternal() {
	}

	public ReservationDataInternal(SecretReservationKeyInternal secretReservationKey,
								   ConfidentialReservationDataInternal confidentialReservationData, String urnPrefix) {
		this.secretReservationKey = secretReservationKey;
		this.confidentialReservationData = confidentialReservationData;
		this.urnPrefix = urnPrefix;
	}

	public ConfidentialReservationDataInternal getConfidentialReservationData() {
		return confidentialReservationData;
	}

	public String getUrnPrefix() {
		return urnPrefix;
	}

	public void setConfidentialReservationData(ConfidentialReservationDataInternal confidentialReservationData) {
		this.confidentialReservationData = confidentialReservationData;
	}

	public void setUrnPrefix(String urnPrefix) {
		this.urnPrefix = urnPrefix;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setSecretReservationKey(SecretReservationKeyInternal secretReservationKey) {
		this.secretReservationKey = secretReservationKey;
	}

	public SecretReservationKeyInternal getSecretReservationKey() {
		return secretReservationKey;

	}

	public Long getId() throws Exception {
		return id;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void delete() {
		this.deleted = true;
	}

	public void unDelete() {
		this.deleted = false;
	}


}
