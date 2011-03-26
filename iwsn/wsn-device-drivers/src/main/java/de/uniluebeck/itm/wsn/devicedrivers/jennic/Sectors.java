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

package de.uniluebeck.itm.wsn.devicedrivers.jennic;

/**
 * @author Markus Class defining sectors
 */
public class Sectors {

	/**
	 * @author Markus Enumeration of for sector indecis
	 */
	public enum SectorIndex {
		/**
		 * First Sector
		 */
		FIRST,
		/**
		 * Second Sector
		 */
		SECOND,
		/**
		 * Third Sector
		 */
		THIRD,
		/**
		 * Fourth Sector
		 */
		FOURTH
	}

	;

	/**
	 * Returns the start of the sector with the given index
	 *
	 * @param index
	 * @return
	 */
	public static int getSectorStart(SectorIndex index) {
		if (index == SectorIndex.FIRST) {
			return 0x00000;
		} else if (index == SectorIndex.SECOND) {
			return 0x08000;
		} else if (index == SectorIndex.THIRD) {
			return 0x10000;
		} else if (index == SectorIndex.FOURTH) {
			return 0x18000;
		} else {
			return -1;
		}
	}

	/**
	 * Returns the end of the sector with given index
	 *
	 * @param index
	 * @return
	 */
	public static int getSectorEnd(SectorIndex index) {
		if (index == SectorIndex.FIRST) {
			return 0x07fff;
		} else if (index == SectorIndex.SECOND) {
			return 0x0ffff;
		} else if (index == SectorIndex.THIRD) {
			return 0x17fff;
		} else if (index == SectorIndex.FOURTH) {
			return 0x1ffff;
		} else {
			return -1;
		}
	}
}
