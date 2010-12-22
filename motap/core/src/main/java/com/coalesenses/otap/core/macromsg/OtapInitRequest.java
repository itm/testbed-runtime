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
public class OtapInitRequest {

	/**
	 * Wrapper class for representing the array element 'participating_devices'.
	 */
	public static class participating_devicesArray {

		/**
		 * This parameter stores the actual array.
		 */
		public int[] value = new int[50];

		/**
		 * This parameter the number of element actually used in the array.
		 */
		public int count;


	}

	/**
	 * Generated from local element 'chunk_count'.
	 */
	public short chunk_count;

	/**
	 * Generated from local element 'timeout_multiplier_ms'.
	 */
	public short timeout_multiplier_ms;

	/**
	 * Generated from local element 'max_re_requests'.
	 */
	public short max_re_requests;

	/**
	 * Parameter storing an instance of the array element 'participating_devices'.
	 */
	public participating_devicesArray participating_devices = new participating_devicesArray();

	/**
	 * Check for object equality.
	 *
	 * @param o The other object.
	 */
/*	@Override
	public boolean equals(Object o) {
		if (!(o instanceof OtapInitRequest)) {
			return false;
		}
		OtapInitRequest other = (OtapInitRequest)o;
		boolean equal = true;
		equal = equal && (this.chunk_count == other.chunk_count);
		equal = equal && (this.timeout_multiplier_ms == other.timeout_multiplier_ms);
		equal = equal && (this.max_re_requests == other.max_re_requests);
		for (int i1 = 0; i1 < this.participating_devices.count; ++i1) {
		equal = equal && (this.participating_devices.value[i1] == other.participating_devices.value[i1]);
		}
		return equal;
	}
*/

}
