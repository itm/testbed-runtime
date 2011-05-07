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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package eu.wisebed.testbed.api.wsn;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;


/**
 * Helper class containing several methods to check method parameter preconditions. Holds a set of served URN prefixes
 * and a set of known node URNs (e.g. the set of node URNs that are part of the current reservation) as its state and
 * runs every check against these two sets.
 */
public class CommonPreconditions {

	/**
	 * The set of known nodes.
	 */
	private ImmutableSet<String> knownNodeUrns = ImmutableSet.of();

	/**
	 * The set of served URN prefixes.
	 */
	private ImmutableSet<String> servedUrnPrefixes = ImmutableSet.of();

	private static final Joiner joiner = Joiner.on(", ");

	/**
	 * Adds {@code knownNodeUrns} to the set of known nodes. All checks regarding known nodes will be run against the
	 * resulting set of known nodes.
	 *
	 * @param knownNodeUrns the known node URNs to add
	 */
	public void addKnownNodeUrns(final String... knownNodeUrns) {
		this.knownNodeUrns = ImmutableSet.<String>builder().addAll(this.knownNodeUrns).add(knownNodeUrns).build();
	}

	/**
	 * Adds {@code servedUrnPrefixes} to the set of served URN prefixes. All checks that test for URN prefix matches will
	 * be run against the resulting set of served URN prefixes.
	 *
	 * @param servedUrnPrefixes the served URN prefixes to add
	 */
	public void addServedUrnPrefixes(String... servedUrnPrefixes) {
		this.servedUrnPrefixes =
				ImmutableSet.<String>builder().addAll(this.servedUrnPrefixes).add(servedUrnPrefixes).build();
	}

	/**
	 * Checks if the prefixes of the node URNs in {@code nodeUrns} are served.
	 *
	 * @param nodeUrns the node URNs to check
	 *
	 * @throws RuntimeException if at least one of the node URNs prefix is not served
	 * @see CommonPreconditions#addServedUrnPrefixes(String...)
	 */
	public void checkNodeUrnsPrefixesServed(String... nodeUrns) {
		checkNodeUrnsPrefixesServed(Sets.<String>newHashSet(nodeUrns));
	}

	/**
	 * Checks if the prefixes of the node URNs in {@code nodeUrns} are served.
	 *
	 * @param nodeUrns the node URNs to check
	 *
	 * @throws RuntimeException if at least one of the node URNs prefix is not served
	 * @see CommonPreconditions#addServedUrnPrefixes(String...)
	 */
	public void checkNodeUrnsPrefixesServed(Collection<String> nodeUrns) {

		Set<String> nodeUrnsOfUnservedPrefixes = Sets.newHashSet();

		for (String nodeUrn : nodeUrns) {

			boolean nodeUrnMatch = false;

			for (String servedUrnPrefix : servedUrnPrefixes) {
				if (nodeUrn.startsWith(servedUrnPrefix)) {
					nodeUrnMatch = true;
				}
			}

			if (!nodeUrnMatch) {
				nodeUrnsOfUnservedPrefixes.add(nodeUrn);
			}
		}

		if (nodeUrnsOfUnservedPrefixes.size() > 0) {
			throw new IllegalArgumentException(
					"Ignoring request as the following node URNs have prefixes not served: " +
							joiner.join(nodeUrnsOfUnservedPrefixes)
			);
		}
	}

	/**
	 * Checks if all URN prefixes in {@code urnPrefixes} are served.
	 *
	 * @param urnPrefixes the URN prefixes to check
	 *
	 * @throws IllegalArgumentException if at least one the URN prefixes is unknown
	 * @see CommonPreconditions#addServedUrnPrefixes(String...)
	 */
	public void checkUrnPrefixesServed(Set<String> urnPrefixes) {

		Set<String> unservedUrnPrefixes = Sets.difference(urnPrefixes, servedUrnPrefixes);

		if (unservedUrnPrefixes.size() > 0) {
			throw new IllegalArgumentException("Ignoring request as the following URN prefixes are not served: " +
					joiner.join(unservedUrnPrefixes)
			);
		}
	}

	/**
	 * Checks if all node URNs in {@code nodeNames} are known (e.g. part of the reservation).
	 *
	 * @param nodeUrns the node URNs to check
	 *
	 * @throws IllegalArgumentException if at least one node URN is not known
	 * @see CommonPreconditions#addKnownNodeUrns(String...)
	 */
	public void checkNodesKnown(final String... nodeUrns) {
		checkNodesKnown(Sets.<String>newHashSet(nodeUrns));
	}

	/**
	 * Checks if all node URNs in {@code nodeNames} are known (e.g. part of the reservation).
	 *
	 * @param nodeUrns the node URNs to check
	 *
	 * @throws IllegalArgumentException if at least one node URN is not known
	 * @see CommonPreconditions#addKnownNodeUrns(String...)
	 */
	public void checkNodesKnown(final Collection<String> nodeUrns) {

		Set<String> unknownNodeUrns =
				Sets.difference(
						(nodeUrns instanceof Set ? (Set<String>) nodeUrns : Sets.<String>newHashSet(nodeUrns)),
						knownNodeUrns
				);

		if (unknownNodeUrns.size() > 0) {
			throw new IllegalArgumentException("Ignoring request as the following nodes are not known or not part of "
					+ "the current reservation: " + joiner.join(unknownNodeUrns)
			);
		}
	}

}
