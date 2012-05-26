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

package de.uniluebeck.itm.tr.nodeapi;

public class MessageType {

	public final static byte DEBUG_MESSAGE = 10;

	public final static byte VL_MESSAGE = 11;

	public final static byte BYTE_MESSAGE = 12;

	public final static byte FLASH_MESSAGE = 13;

	public final static byte ENABLE_NODE = 20;

	public final static byte DISABLE_NODE = 21;

	public final static byte RESET_NODE = 22;

	public final static byte SET_START_TIME = 23;

	public final static byte SET_VIRTUAL_ID = 24;

	public final static byte IS_NODE_ALIVE = 25;

	public final static byte GET_ID = 26;

	public final static byte SET_VIRTUAL_LINK = 30;

	public final static byte DESTROY_VIRTUAL_LINK = 31;

	public final static byte ENABLE_PHYSICAL_LINK = 32;

	public final static byte DISABLE_PHYSICAL_LINK = 33;

	public final static byte SEND_VIRTUAL_LINK_MESSAGE = 34;

	public final static byte GET_PROPERTY_VALUE = 40;

	public final static byte GET_NEIGHBORHOOD = 41;

	public static boolean isMessageTypeLegal(byte messageType) {
		return !toString(messageType).startsWith("Error");
	}

	public static String toString(byte mt) {
		switch (mt) {
			case DEBUG_MESSAGE: {
				return "DEBUG_MESSAGE";
			}
			case VL_MESSAGE: {
				return "VL_MESSAGE";
			}
			case BYTE_MESSAGE: {
				return "BYTE_MESSAGE";
			}
			case FLASH_MESSAGE: {
				return "FLASH_MESSAGE";
			}
			case ENABLE_NODE: {
				return "ENABLE_NODE";
			}
			case DISABLE_NODE: {
				return "DISABLE_NODE";
			}
			case RESET_NODE: {
				return "RESET_NODE";
			}
			case SET_START_TIME: {
				return "SET_START_TIME";
			}
			case SET_VIRTUAL_ID: {
				return "SET_VIRTUAL_ID";
			}
			case IS_NODE_ALIVE: {
				return "IS_NODE_ALIVE";
			}
			case SET_VIRTUAL_LINK: {
				return "SET_VIRTUAL_LINK";
			}
			case DESTROY_VIRTUAL_LINK: {
				return "DESTROY_VIRTUAL_LINK";
			}
			case ENABLE_PHYSICAL_LINK: {
				return "ENABLE_PHYSICAL_LINK";
			}
			case DISABLE_PHYSICAL_LINK: {
				return "DISABLE_PHYSICAL_LINK";
			}
			case SEND_VIRTUAL_LINK_MESSAGE: {
				return "SEND_VIRTUAL_LINK_MESSAGE";
			}
			case GET_PROPERTY_VALUE: {
				return "GET_PROPERTY_VALUE";
			}
			case GET_NEIGHBORHOOD: {
				return "GET_NEIGHBORHOOD";
			}
			default: {
				return "Error " + mt + " is not a valid packet type";
			}
		}
	}

}
