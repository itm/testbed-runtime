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
import java.util.Map;

@Entity
public class ConfidentialReservationDataInternal extends PublicReservationDataInternal implements Serializable {

	@Column
	protected String urnPrefix;

	@Column
	protected String username;

	@Column
	private String secretReservationKey;

	@Column
	private String description;

	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, String> options;

	public ConfidentialReservationDataInternal() {
		super();
	}

	public ConfidentialReservationDataInternal(final long fromDate,
											   final long toDate,
											   @Nullable final Long cancelledDate,
											   final List<String> nodeUrns,
											   final String description,
											   final String secretReservationKey,
											   final Map<String, String> options,
											   final String urnPrefix,
											   final String username) {
		super(fromDate, toDate, cancelledDate, nodeUrns);
		this.description = description;
		this.secretReservationKey = secretReservationKey;
		this.options = options;
		this.urnPrefix = urnPrefix;
		this.username = username;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(final Map<String, String> options) {
		this.options = options;
	}

	public String getSecretReservationKey() {
		return secretReservationKey;
	}

	public void setSecretReservationKey(final String key) {
		this.secretReservationKey = key;
	}

	public String getUrnPrefix() {
		return urnPrefix;
	}

	public void setUrnPrefix(final String urnPrefix) {
		this.urnPrefix = urnPrefix;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	@Override
	public String toString() {
		return "ConfidentialReservationDataInternal{" +
				"username='" + username + '\'' +
				", urnPrefix='" + urnPrefix + '\'' +
				", secretReservationKey='" + secretReservationKey + '\'' +
				", options=" + options +
				", description='" + description + '\'' +
				"} " + super.toString();
	}
}
