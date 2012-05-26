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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import de.uniluebeck.itm.tr.util.Service;


/**
 * A simple naming service that allows for storing and retrieving naming entries that can contain various different
 * network interfaces for a given name.
 */
public interface NamingService extends Service {

	/**
	 * Adds an entry to the naming service.
	 *
	 * @param entry the entry to add to the naming service
	 */
	void addEntry(NamingEntry entry);

	/**
	 * Removes a naming entry from the naming service.
	 *
	 * @param entry the entry to remove
	 */
	void removeEntry(NamingEntry entry);

	/**
	 * Returns all entries of the naming table.
	 *
	 * @return a set of all naming entries of the naming service
	 */
	ImmutableSet<NamingEntry> getEntries();

	/**
	 * Returns all entries of the naming table for the node {@code nodeName}.
	 *
	 * @param nodeName the nodes' name whose entries are to be retrieved
	 * @return a set containing all naming table entries for the node {@code nodeName}
	 */
	ImmutableSortedSet<NamingEntry> getEntries(String nodeName);

	/**
	 * Returns the naming entry with the highest priority for the given node name.
	 *
	 * @param name the nodes' name
	 * @return the naming entry with the highest priority or {@code null} if there's no entry available
	 */
	NamingEntry getEntry(String name);

}