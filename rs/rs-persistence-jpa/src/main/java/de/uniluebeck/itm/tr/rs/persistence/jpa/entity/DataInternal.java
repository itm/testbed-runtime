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
@Table(name = "Users")
public class DataInternal implements Serializable {

	@Id
	@GeneratedValue
	@Column(nullable = false)
	private long id;

	@Column
	protected String urnPrefix;

	@Column
	protected String username;

    @Column
    private String secretReservationKey;

	// @ManyToOne
	// @JoinColumn(name="confidentialReservationData_id", nullable=false)
	// protected ConfidentialReservationDataInternal confidentialReservationData;

	public DataInternal() {
	}

	public DataInternal(String urnPrefix, String username, String secretReservationKey) {
		this.urnPrefix = urnPrefix;
		this.username = username;
        this.secretReservationKey = secretReservationKey;
	}

	public String getUrnPrefix() {
		return urnPrefix;
	}

	public void setUrnPrefix(String urnPrefix) {
		this.urnPrefix = urnPrefix;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSecretReservationKey() {
        return secretReservationKey;
    }

    public void setSecretReservationKey(String secretReservationKey) {
        this.secretReservationKey = secretReservationKey;
    }

    @Override
	public String toString() {
		return "DataInternal{" +
				"urnPrefix='" + urnPrefix + '\'' +
				", username='" + username + '\'' +
				", secretReservationKey='" + secretReservationKey + '\'' +
				'}';
	}

}
