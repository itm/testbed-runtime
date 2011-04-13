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
 * Contains constants to identify the packet types ({@link de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket#getType()}.
 */
public class PacketTypes {


	// Uart packet types

	/**
	 *
	 */
	public static final int ISENSE_ISHELL_INTERPRETER = 0;

	/**
	 *
	 */
	public static final int RESET = 1;

	/**
	 *
	 */
	public static final int SERAERIAL = 2;

	/**
	 *
	 */
	public static final int TIMERESPONSE = 3;

	/**
	 *
	 */
	public static final int CAMERA_APPLICATION = 4;

	/**
	 *
	 */
	public static final int FUNCTIONTEST = 4;

	/**
	 *
	 */
	public static final int AMR_APPLICATION = 5;

	/**
	 *
	 */
	public static final int ACC_APPLICATION = 6;

	/**
	 *
	 */
	public static final int OUT_VIRTUAL_RADIO = 7;

	/**
	 *
	 */
	public static final int OUT_RESERVED_2 = 8;

	/**
	 *
	 */
	public static final int OUT_RESERVED_3 = 9;

	/**
	 *
	 */
	public static final int CUSTOM_OUT_1 = 10;

	/**
	 *
	 */
	public static final int CUSTOM_OUT_2 = 11;

	/**
	 *
	 */
	public static final int CUSTOM_OUT_3 = 12;


	// Radio packet types

	/**
	 *
	 */
	public static final int HIBERNATION = 5;

	/**
	 *
	 */
	public static final int OTAP = 6;

	/**
	 *
	 */
	public static final int DATA_EXCHANGER = 25;

	/**
	 *
	 */
	public static final int LOG = 104;

	/**
	 * Corresponds to the first byte of the {@link de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket#getContent()}
	 */
	public static class LogType {

		/**
		 *
		 */
		public final static int DEBUG = 0;

		/**
		 *
		 */
		public final static int FATAL = 1;

	}

	/**
	 *
	 */
	public static final int PLOT = 105;

	/**
	 *
	 */
	public static final int FLASH_DUMP = 106;

	/**
	 *
	 */
	public static final int PLOTX = 107;

	/**
	 *
	 */
	public static final int JPEG = 108;

	/**
	 *
	 */
	public static final int TIMEREQUEST = 109;

	/**
	 *
	 */
	public static final int AUDIO = 110;

	/**
	 * UART Message Type for incoming SpyGlass Packets
	 */
	public static final int SPYGLASS = 111;

	/**
	 *
	 */
	public static final int FLOATBUFFER = 112;

	/**
	 *
	 */
	public static final int ISENSE_ISI_PACKET_TYPE_ISENSE_ID = 113;

	/**
	 * UART Message Type for incoming virtual radio communication from the node
	 */
	public static final int IN_VIRTUAL_RADIO = 114;

	// from tiny os

	/**
	 * No Packet Type - no framing 
	 */
	public static final int TOS_AMTYPE_PRINTF = 0x64; // = 100


	/**
	 *
	 */
	public class ISenseCommands {

		/**
		 *
		 */
		public static final int ISENSE_ISI_COMMAND_SET_CHANNEL = 2;

		/**
		 *
		 */
		public static final int ISENSE_ISI_COMMAND_SEND_ID_TO_ISHELL = 3;

		/**
		 *
		 */
		public static final int ISENSE_ISI_COMMAND_ISHELL_TO_ROUTING = 4;

	}

	/**
	 *
	 */
	public class ISenseRoutings {

		/**
		 *
		 */
		public static final int ISENSE_ISI_ROUTING_TREE_ROUTING = 7;

	}

}
