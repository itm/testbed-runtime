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

package de.uniluebeck.itm.tr.util;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An immutable tuple type that takes two elements of type {@code V} and type {@code W}.
 *
 * @param <V> the type of the first element of the tuple
 * @param <W> the type of the second element of the tuple
 */
public class Tuple<V, W> {

	/**
	 * The first element
	 */
	private V first;

	/**
	 * The second element
	 */
	private W second;

	/**
	 * Constructs a new immutable tuple.
	 *
	 * @param first  the first element of the tuple
	 * @param second the second element of the tuple
	 */
	public Tuple(V first, W second) {

		checkNotNull(first);
		checkNotNull(second);

		this.first = first;
		this.second = second;
	}

	/**
	 * Returns the first element of this tuple.
	 *
	 * @return the first element of this tuple
	 */
	public V getFirst() {
		return first;
	}

	/**
	 * Returns the second element of this tuple.
	 *
	 * @return the second element of this tuple
	 */
	public W getSecond() {
		return second;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Tuple tuple = (Tuple) o;

		if (!first.equals(tuple.first)) {
			return false;
		}
		if (!second.equals(tuple.second)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = first.hashCode();
		result = 31 * result + second.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Tuple{" +
				"first=" + first +
				", second=" + second +
				'}';
	}

}
