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

package de.uniluebeck.itm.wsn.devicedrivers.generic;


/**
 * Enum of ChipTypes *
 */
public enum ChipType {
	/**
	 * Mode for JN5121 platform
	 */
	JN5121,
	/**
	 * Mode for JN513X platform
	 */
	JN513X,
	/**
	 * Mode for JN513XR1 platform
	 */
	JN513XR1,
	/**
	 * Mode for JN5148 platform
	 */
	Shawn,
	/**
	 * Mode for Telos Revision B (TelosB) platform
	 */
	JN5148,
	/**
	 * Mode for Shawn simulator
	 */
	TelosB,
	/**
	 * Mode for Pacemate LPC2136 platform
	 */
	LPC2136,
	/**
	 * Mode for unknown platform
	 */
	Unknown;

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	public static int getHeaderStart(ChipType chipType) {
		int headerStart = -1;

		if (chipType == ChipType.JN5121 | chipType == ChipType.JN513X)
			headerStart = 0x24;
		else if (chipType == ChipType.JN513XR1)
			headerStart = 0x24 + 0x0C;
		else if (chipType == ChipType.JN5148)
			headerStart = 0x24 + 0x0C;

		return headerStart;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	public static int getHeaderLength(ChipType chipType) {
		int headerLength = -1;

		if (chipType == ChipType.JN5121 | chipType == ChipType.JN513X)
			headerLength = 0x20;
		else if (chipType == ChipType.JN513XR1)
			headerLength = 0x20;
		else if (chipType == ChipType.JN5148)
			headerLength = 0x20;

		return headerLength;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	public static int getMacInFlashStart(ChipType chipType) {
		switch (chipType) {
			case JN5121:
			case JN513X:
				return 0x24;
			case JN513XR1:
				return 0x30;
			case JN5148:
				return 0x30;
		}
		return -1;
	}

	/**
	 * Returns the ChipType for a given short
	 *
	 * @param chipType
	 * @return
	 */
	public static ChipType getChipType(short chipType) {
		if (chipType == 0)
			return ChipType.JN5121;
		if (chipType == 1)
			return ChipType.JN513X;
		if (chipType == 2)
			return ChipType.JN513XR1;
		if (chipType == 3)
			return ChipType.Shawn;
		if (chipType == 4)
			return ChipType.JN5148;
		if (chipType == 5)
			return ChipType.TelosB;
		if (chipType == 6)
			return ChipType.LPC2136;
		return ChipType.Unknown;
	}


	/**
	 * Returns the String representation of a given ChipType
	 *
	 * @param chipType
	 * @return
	 */
	public static String toString(ChipType chipType) {
		switch (chipType) {
			case JN5121:
				return "JN5121";
			case JN513X:
				return "JN513x";
			case JN513XR1:
				return "JN513xR1";
			case Shawn:
				return "Shawn";
			case JN5148:
				return "JN5148";
			case TelosB:
				return "Telos Rev B";
			case LPC2136:
				return "LPC2136 Pacemate";
		}
		return "Unknown";
	}
}
