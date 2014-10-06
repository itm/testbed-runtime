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

import javax.annotation.Nullable;
import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class PublicReservationDataInternal implements Serializable {

	@Id
	@GeneratedValue
	@Column(nullable = false)
	private long id;

	@Column(nullable = false)
	protected long fromDate;

	@Column(nullable = false)
	protected long toDate;

	@Column(nullable = true)
	protected Long cancelledDate;

	@Column(nullable = true)
	protected Long finalizedDate;

	public PublicReservationDataInternal() {
	}

	public PublicReservationDataInternal(long fromDate, long toDate, @Nullable Long cancelledDate,
										 @Nullable Long finalizedDate, List<String> nodeUrns) {
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.finalizedDate = finalizedDate;
		this.nodeUrns = nodeUrns;
		this.cancelledDate = cancelledDate;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@ElementCollection(targetClass = java.lang.String.class)
	@JoinTable(name = "reservationdata_urns", joinColumns = @JoinColumn(name = "urn_id"))
	@OrderColumn(name = "POSITION")
	@Column(name = "urns", nullable = false)
	private List<String> nodeUrns;

	public long getFromDate() {
		return fromDate;
	}

	public void setFromDate(long value) {
		this.fromDate = value;
	}

	public List<String> getNodeUrns() {
		return this.nodeUrns;
	}

	public void setNodeUrns(List<String> nodeUrns) {
		this.nodeUrns = nodeUrns;
	}

	public long getToDate() {
		return toDate;
	}

	public void setToDate(long value) {
		this.toDate = value;
	}

	@Nullable
	public Long getCancelledDate() {
		return cancelledDate;
	}

	public void setCancelledDate(@Nullable final Long cancelledDate) {
		this.cancelledDate = cancelledDate;
	}

	@Nullable
	public Long getFinalizedDate() {
		return finalizedDate;
	}

	public void setFinalizedDate(@Nullable final Long finalizedDate) {
		this.finalizedDate = finalizedDate;
	}
}
