/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

package de.uniluebeck.itm.tr.util.persistentBlockingQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

public interface PersistentBlockingDeque extends BlockingDeque {

    //Methods for concurrent.BlockingDeque

    @Override
    public void addFirst(Object o);

    @Override
    public void addLast(Object o) throws IllegalStateException;

    @Override
    public boolean offerFirst(Object o);

    @Override
    public boolean offerLast(Object o);

    @Override
    public void putFirst(Object o) throws InterruptedException;

    @Override
    public void putLast(Object o) throws InterruptedException;

    @Override
    public boolean offerFirst(Object o, long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    public boolean offerLast(Object o, long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    public Object takeFirst() throws InterruptedException;

    @Override
    public Object takeLast() throws InterruptedException;

    @Override
    public Object pollFirst(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    public Object pollLast(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    public boolean removeFirstOccurrence(Object o);

    @Override
    public boolean removeLastOccurrence(Object o);

    @Override
    public boolean add(Object o);

    @Override
    public boolean offer(Object o);

    @Override
    public void put(Object o) throws InterruptedException;

    @Override
    public boolean offer(Object o, long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    public Object remove();

    @Override
    public Object poll();

    @Override
    public Object take() throws InterruptedException;

    @Override
    public Object poll(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    public Object element();

    @Override
    public Object peek();

    @Override
    public boolean remove(Object o);

    @Override
    public boolean contains(Object o);

    @Override
    public int size();

    @Override
    public Iterator iterator();

    @Override
    public void push(Object o);

        //Methods for concurent.BlockingQueue

    @Override
    public int remainingCapacity();

    @Override
    public int drainTo(Collection c);

    @Override
    public int drainTo(Collection c, int maxElements);

           //Methods for util.Deque

    @Override
    public Object removeFirst();

    @Override
    public Object removeLast();

    @Override
    public Object pollFirst();

    @Override
    public Object pollLast();

    @Override
    public Object getFirst();

    @Override
    public Object getLast();

    @Override
    public Object peekFirst();

    @Override
    public Object peekLast();

    @Override
    public Object pop();

    @Override
    public Iterator descendingIterator();

     //Methods for util.Collection


    @Override
    public boolean isEmpty();

    @Override
    public Object[] toArray();

    @Override
    public boolean containsAll(Collection c);

    @Override
    public boolean addAll(Collection c);

    @Override
    public boolean removeAll(Collection c);

    @Override
    public boolean retainAll(Collection c);

    @Override
    public void clear();

    @Override
    public Object[] toArray(Object[] a);

}
