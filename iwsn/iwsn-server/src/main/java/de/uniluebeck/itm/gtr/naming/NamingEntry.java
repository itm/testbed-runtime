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

package de.uniluebeck.itm.gtr.naming;

import java.util.Comparator;


public class NamingEntry implements Cloneable {

	public static class NamingEntryComparator implements Comparator<NamingEntry> {
		@Override
		public int compare(NamingEntry o1, NamingEntry o2) {
			return o1.priority < o2.priority ? -1 : o1.priority == o2.priority ? 0 : 1;
		}
	}

	private String nodeName;

	private NamingInterface iface;

	private int priority;

	public NamingEntry(String nodeName, NamingInterface iface, int priority) {
		this.nodeName = nodeName;
		this.iface = iface;
		this.priority = priority;
	}

	public String getNodeName() {
		return nodeName;
	}

	public NamingInterface getIface() {
		return iface;
	}

	public int getPriority() {
		return priority;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NamingEntry that = (NamingEntry) o;

		if (priority != that.priority) return false;
		if (iface != null ? !iface.equals(that.iface) : that.iface != null) return false;
		if (nodeName != null ? !nodeName.equals(that.nodeName) : that.nodeName != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = nodeName != null ? nodeName.hashCode() : 0;
		result = 31 * result + (iface != null ? iface.hashCode() : 0);
		result = 31 * result + priority;
		return result;
	}

	@Override
	public String toString() {
		return "NamingEntry{" +
				"nodeName='" + nodeName + '\'' +
				", iface=" + iface +
				", priority=" + priority +
				'}';
	}

}
