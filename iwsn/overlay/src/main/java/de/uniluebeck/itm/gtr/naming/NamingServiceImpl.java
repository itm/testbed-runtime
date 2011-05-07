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
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Singleton
class NamingServiceImpl implements NamingService {

	private static final Logger log = LoggerFactory.getLogger(NamingService.class);

	private final Map<String, SortedSet<NamingEntry>> namingTable = new HashMap<String, SortedSet<NamingEntry>>();

	@Override
	public void addEntry(NamingEntry entry) {

		synchronized (namingTable) {

			SortedSet<NamingEntry> entries = namingTable.get(entry.getNodeName());

			if (entries == null) {
				entries = new TreeSet<NamingEntry>(new NamingEntry.NamingEntryComparator());
				namingTable.put(entry.getNodeName(), entries);
			}

			if (!entries.contains(entry)) {
				entries.add(entry);
			}

			log.debug("Added naming entry: {}", entry);

		}

	}

	@Override
	public void removeEntry(NamingEntry entry) {

		synchronized (namingTable) {

			SortedSet<NamingEntry> entries = namingTable.get(entry.getNodeName());

			for (Iterator<NamingEntry> iterator = entries.iterator(); iterator.hasNext();) {
				NamingEntry namingEntry = iterator.next();
				if (namingEntry.equals(entry)) {
					iterator.remove();
				}
			}

			log.debug("Removed naming entry: {}", entry);
			log.debug("New naming table contents: {}", namingTable);

		}

	}

	@Override
	public ImmutableSet<NamingEntry> getEntries() {

		ImmutableSet.Builder<NamingEntry> builder = ImmutableSet.builder();

		for (SortedSet<NamingEntry> entries : namingTable.values()) {
			for (NamingEntry entry : entries) {
				builder.add(entry);
			}
		}

		return builder.build();

	}

	@Override
	public ImmutableSortedSet<NamingEntry> getEntries(String nodeName) {

		SortedSet<NamingEntry> entry = namingTable.get(nodeName);
		if (entry == null) {
			log.debug("Didn't find a naming entry for node name \"{}\"", nodeName);
			return null;
		}
		return ImmutableSortedSet.copyOf(entry);

	}

	@Override
	public NamingEntry getEntry(String name) {

		SortedSet<NamingEntry> entries = namingTable.get(name);
		return entries == null ? null : entries.size() == 0 ? null : entries.first();

	}

	@Override
	public void start() throws Exception {
		// nothing to do
	}

	@Override
	public void stop() {
		// nothing to do
	}
}
