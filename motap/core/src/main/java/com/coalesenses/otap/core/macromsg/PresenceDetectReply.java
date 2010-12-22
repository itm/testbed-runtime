/**********************************************************************************************************************
 * Copyright (c) 2010, coalesenses GmbH                                                                               *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the coalesenses GmbH nor the names of its contributors may be used to endorse or promote     *
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

package com.coalesenses.otap.core.macromsg;

/**
 * This class is representing a local complex type embedding in a top-level element.
 */
public class PresenceDetectReply {

	static int header_length = 7;

	/**
	 * Generated from local element 'device_id'.
	 */
	public int device_id;


	public short chip_type;


	public int application_id;

	/**
	 * Generated from local element 'revision_no'.
	 */
	public short revision_no;


	public short protocol_version;


	/**
	 * Check for object equality.
	 *
	 * @param o The other object.
	 */
	/*@Override
	public boolean equals(Object o) {
		if (!(o instanceof PresenceDetectReply)) {
			return false;
		}
		PresenceDetectReply other = (PresenceDetectReply)o;
		boolean equal = true;
		equal = equal && (this.device_id == other.device_id);
		equal = equal && (this.chip_type == other.chip_type);
		equal = equal && (this.application_id == other.application_id);
		equal = equal && (this.revision_no == other.revision_no);
		equal = equal && (this.protocol_version == other.protocol_version);
		return equal;
	}
	*/

}
