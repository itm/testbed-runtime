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
				name = ReservationDataInternal.QGetByReservationKey.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " data.secretReservationKey.secretReservationKey = :" + ReservationDataInternal.QGetByReservationKey.P_SECRET_RESERVATION_KEY
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetByInterval.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " :" + ReservationDataInternal.QGetByInterval.P_TO + " > data.confidentialReservationData.fromDate"
						+ " AND"
						+ " ("
						+ "   ("
						+ "     data.confidentialReservationData.cancelledDate IS NULL"
						+ "      AND"
						+ "     :" + ReservationDataInternal.QGetByInterval.P_FROM + " < data.confidentialReservationData.toDate"
						+ "   )"
						+ "   OR"
						+ "   :" + ReservationDataInternal.QGetByInterval.P_FROM + " < data.confidentialReservationData.cancelledDate"
						+ " )"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"

		),
		@NamedQuery(
				name = ReservationDataInternal.QGetByIntervalWithoutCancelled.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " data.confidentialReservationData.cancelledDate IS NULL"
						+ "  AND"
						+ " :" + ReservationDataInternal.QGetByIntervalWithoutCancelled.P_TO + " > data.confidentialReservationData.fromDate"
						+ "  AND"
						+ " :" + ReservationDataInternal.QGetByIntervalWithoutCancelled.P_FROM + " < data.confidentialReservationData.toDate"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"

		),
		@NamedQuery(
				name = ReservationDataInternal.QGetActive.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " data.confidentialReservationData.fromDate <= :" + ReservationDataInternal.QGetActive.P_NOW
						+ " AND ("
						+ "  ("
						+ "   data.confidentialReservationData.cancelledDate IS NULL"
						+ "    AND"
						+ "   data.confidentialReservationData.toDate > :" + ReservationDataInternal.QGetActive.P_NOW
						+ "  ) OR ("
						+ "   data.confidentialReservationData.cancelledDate > :" + ReservationDataInternal.QGetActive.P_NOW
						+ "  )"
						+ " )"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetFuture.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " data.confidentialReservationData.fromDate > :" + ReservationDataInternal.QGetFuture.P_NOW
						+ "  AND ("
						+ "   data.confidentialReservationData.cancelledDate IS NULL"
						+ "    OR"
						+ "   data.confidentialReservationData.cancelledDate > data.confidentialReservationData.fromDate"
						+ "  )"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetActiveAndFuture.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " (" // active
						+ "  data.confidentialReservationData.fromDate <= :" + ReservationDataInternal.QGetActive.P_NOW
						+ "  AND ("
						+ "   ("
						+ "    data.confidentialReservationData.cancelledDate IS NULL"
						+ "     AND"
						+ "    data.confidentialReservationData.toDate > :" + ReservationDataInternal.QGetActive.P_NOW
						+ "   ) OR ("
						+ "    data.confidentialReservationData.cancelledDate > :" + ReservationDataInternal.QGetActive.P_NOW
						+ "   )"
						+ "  )"
						+ " )"
						+ " OR"
						+ " (" // future
						+ "  data.confidentialReservationData.fromDate > :" + ReservationDataInternal.QGetFuture.P_NOW
						+ "   AND ("
						+ "    data.confidentialReservationData.cancelledDate IS NULL"
						+ "     OR"
						+ "    data.confidentialReservationData.cancelledDate > data.confidentialReservationData.fromDate"
						+ "   )"
						+ " )"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetAll.QUERY_NAME,
				query = "FROM ReservationDataInternal data"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetAllWithoutCancelled.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " data.confidentialReservationData.cancelledDate IS NULL"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetTo.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " ("
						+ "  data.confidentialReservationData.cancelledDate IS NULL"
						+ "   AND "
						+ "  data.confidentialReservationData.toDate <= :" + ReservationDataInternal.QGetTo.P_TO
						+ " )"
						+ " OR"
						+ " ("
						+ "  data.confidentialReservationData.cancelledDate IS NOT NULL"
						+ "   AND"
						+ "  data.confidentialReservationData.cancelledDate <= :" + ReservationDataInternal.QGetTo.P_TO
						+ " )"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetToWithoutCancelled.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " ("
						+ "  data.confidentialReservationData.cancelledDate IS NULL"
						+ "   AND "
						+ "  data.confidentialReservationData.toDate <= :" + ReservationDataInternal.QGetToWithoutCancelled.P_TO
						+ " )"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
        @NamedQuery(
                name = ReservationDataInternal.QGetNonFinalized.QUERY_NAME,
                query = "FROM ReservationDataInternal data WHERE"
                        + "  data.confidentialReservationData.finalizedDate IS NULL"
        ),
		@NamedQuery(
				name = ReservationDataInternal.QGetFrom.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " data.confidentialReservationData.toDate > :" + ReservationDataInternal.QGetFrom.P_FROM
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetFromWithoutCancelled.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " ("
						+ "  data.confidentialReservationData.cancelledDate IS NULL"
						+ "   AND"
						+ "  data.confidentialReservationData.toDate > :" + ReservationDataInternal.QGetFromWithoutCancelled.P_FROM
						+ " )"
						+ " ORDER BY data.confidentialReservationData.fromDate DESC, data.confidentialReservationData.toDate DESC"
		),
		@NamedQuery(
				name = ReservationDataInternal.QGetByNodeAndTime.QUERY_NAME,
				query = "FROM ReservationDataInternal data WHERE"
						+ " data.confidentialReservationData.fromDate < :" + ReservationDataInternal.QGetByNodeAndTime.P_TIMESTAMP
						+ " AND"
						+ " ("
						+ "   ("
						+ "    data.confidentialReservationData.cancelledDate IS NULL"
						+ "     AND"
						+ "    data.confidentialReservationData.toDate > :" + ReservationDataInternal.QGetByNodeAndTime.P_TIMESTAMP
						+ "   )"
						+ "   OR"
						+ "   ("
						+ "    data.confidentialReservationData.cancelledDate > :" + ReservationDataInternal.QGetByNodeAndTime.P_TIMESTAMP
						+ "   )"
						+ " )"
						+ " AND"
						+ " :" + ReservationDataInternal.QGetByNodeAndTime.P_NODE_URN + " MEMBER OF data.confidentialReservationData.nodeUrns"
		)
})
public class ReservationDataInternal implements Serializable {

	public static class QGetByNodeAndTime {

		public static final String QUERY_NAME = "getByNodeAndTime";

		public static final String P_NODE_URN = "nodeUrn";

		public static final String P_TIMESTAMP = "timestamp";
	}

	public static class QGetByReservationKey {

		public static final String QUERY_NAME = "getReservationDataBySecretReservationKey";

		public static final String P_SECRET_RESERVATION_KEY = "secretReservationKey";
	}

	public static class QGetByInterval {

		public static final String QUERY_NAME = "getReservationDataByInterval";

		public static final String P_FROM = "from";

		public static final String P_TO = "to";
	}

	public static class QGetByIntervalWithoutCancelled {

		public static final String QUERY_NAME = "getReservationDataByIntervalWithoutCancelled";

		public static final String P_FROM = "from";

		public static final String P_TO = "to";
	}

	public static class QGetActive {

		public static final String QUERY_NAME = "getActiveReservations";

		public static final String P_NOW = "now";
	}

	public static class QGetFuture {

		public static final String QUERY_NAME = "getFutureReservations";

		public static final String P_NOW = "now";
	}

	public static class QGetActiveAndFuture {

		public static final String QUERY_NAME = "getActiveAndFutureReservations";

		public static final String P_NOW = "now";
	}

    public static class QGetNonFinalized {

        public static final String QUERY_NAME = "getNonFinalizedReservations";

    }

	public static class QGetAll {

		public static final String QUERY_NAME = "getReservationDataAll";
	}

	public static class QGetAllWithoutCancelled {

		public static final String QUERY_NAME = "getReservationDataAllWithoutCancelled";
	}

	public static class QGetTo {

		public static final String QUERY_NAME = "getReservationDataTo";

		public static final String P_TO = "to";
	}

	public static class QGetToWithoutCancelled {

		public static final String QUERY_NAME = "getReservationDataToWithoutCancelled";

		public static final String P_TO = "to";
	}

	public static class QGetFrom {

		public static final String QUERY_NAME = "getReservationDataFrom";

		public static final String P_FROM = "from";
	}

	public static class QGetFromWithoutCancelled {

		public static final String QUERY_NAME = "getReservationDataFromWithoutCancelled";

		public static final String P_FROM = "from";
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
}
