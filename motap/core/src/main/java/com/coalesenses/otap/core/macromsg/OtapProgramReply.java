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
public class OtapProgramReply {

	static int header_length = 4;

	/**
	 * Wrapper class for representing the array element 'missing_indices'.
	 */
	public static class missing_indicesArray {

		/**
		 * This parameter stores the actual array.
		 */
		public short[] value = new short[64];

		/**
		 * This parameter the number of element actually used in the array.
		 */
		public short count;


	}

	/**
	 * Generated from local element 'device_id'.
	 */
	public int device_id;


	/**
	 * Generated from local element 'chunk_no'.
	 */
	public short chunk_no;

	/**
	 * Parameter storing an instance of the array element 'missing_indices'.
	 */
	public missing_indicesArray missing_indices = new missing_indicesArray();

	/**
	 * Check for object equality.
	 *
	 * @param o The other object.
	 */
	/*@Override
	public boolean equals(Object o) {
		if (!(o instanceof OtapProgramReply)) {
			return false;
		}
		OtapProgramReply other = (OtapProgramReply)o;
		boolean equal = true;
		equal = equal && (this.device_id == other.device_id);
		equal = equal && (this.chunk_no == other.chunk_no);
		for (int i1 = 0; i1 < this.missing_indices.count; ++i1) {
		equal = equal && (this.missing_indices.value[i1] == other.missing_indices.value[i1]);
		}
		return equal;
	}
	*/

}
