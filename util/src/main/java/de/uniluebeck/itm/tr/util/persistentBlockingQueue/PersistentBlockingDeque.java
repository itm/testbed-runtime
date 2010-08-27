package de.uniluebeck.itm.tr.util.persistentBlockingQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 11.08.2010
 * Time: 17:26:10
 * To change this template use File | Settings | File Templates.
 */
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
