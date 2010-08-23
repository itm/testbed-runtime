package de.uniluebeck.itm.tr.util.persistentQueue;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 10.08.2010
 * Time: 16:38:20
 * To change this template use File | Settings | File Templates.
 */
public interface PersistentQueue <E extends Serializable> {

    public boolean add(E object) throws NotEnoughMemoryException, LongOverflowException;

    public E poll() throws IOException, ClassNotFoundException, SecurityException;

    public E peek() throws IOException, ClassNotFoundException;

    public boolean isEmpty();

    public long size();

    public long getUsedDiskSpaceInByte();
    
}
