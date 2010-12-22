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
public class OtapProgramRequest {

	/**
	 * Wrapper class for representing the array element 'code'.
	 */
	public static class codeArray {

		/**
		 * This parameter stores the actual array.
		 */
		public byte[] value = new byte[64];

		/**
		 * This parameter the number of element actually used in the array.
		 */
		public short count;


	}

	/**
	 * Generated from local element 'chunk_no'.
	 */
	public short chunk_no;

	/**
	 * Generated from local element 'packets_in_chunk'.
	 */
	public short packets_in_chunk;

	/**
	 * Generated from local element 'index'.
	 */
	public short index;

	/**
	 * Generated from local element 'remaining'.
	 */
	public short remaining;

	/**
	 * Generated from local element 'overall_packet_no'.
	 */
	public int overall_packet_no;

	/**
	 * Parameter storing an instance of the array element 'code'.
	 */
	public codeArray code = new codeArray();

	/**
	 * Check for object equality.
	 *
	 * @param o The other object.
	 */
	/*
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof OtapProgramRequest)) {
			return false;
		}
		OtapProgramRequest other = (OtapProgramRequest)o;
		boolean equal = true;
		equal = equal && (this.chunk_no == other.chunk_no);
		equal = equal && (this.packets_in_chunk == other.packets_in_chunk);
		equal = equal && (this.index == other.index);
		equal = equal && (this.remaining == other.remaining);
		equal = equal && (this.overall_packet_no == other.overall_packet_no);
		for (int i1 = 0; i1 < 64; ++i1) {
			equal = equal && (this.code.value[i1] == other.code.value[i1]);
		}
		return equal;
	}
	*/

}
