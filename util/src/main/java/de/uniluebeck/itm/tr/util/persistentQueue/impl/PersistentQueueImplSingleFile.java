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

package de.uniluebeck.itm.tr.util.persistentQueue.impl;

import de.uniluebeck.itm.tr.util.persistentQueue.LongOverflowException;
import de.uniluebeck.itm.tr.util.persistentQueue.NotEnoughMemoryException;
import de.uniluebeck.itm.tr.util.persistentQueue.PersistentQueue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.LinkedList;

/**
 * A concurrent persistent queue. It keeps a copy of its status in a file on disk
 * which is updated every time the queue contents are modified. Therefore, the data
 * in the queue can survive program or system crashes. The name of the status file
 * is given when calling the constructor.
 * <P>
 * The type of elements held in the queue are determined by the type parameter
 * <code>E</code> of this class. The type <code>E</code> has to extend the
 * <code>java.io.Serializable</code> interface so that the entries can be
 * written to the file underlying each instance.
 * <P>
 * <i>Defragmentation</i>: When the first element of the queue is deleted, not
 * the entire file is written. Instead, a <code>PersistentQueueDeleteMarker</code>
 * is appended to the end of the file to signal that the first element has been
 * deleted. However, after some number of remove operations (by default, this is 50,
 * but that can be changed via an optional parameter at instantiation), the entire
 * file is defragmented: a temporary file is written with all contents of the
 * queue. It is then renamed to match the name of the original file. The name of
 * the temporary file is the original filename plus '.temp'.
 * <p>
 * More information can be found at
 * <A HREF="http://www.gaborcselle.com/writings/java/persistent_queue.html">the author's website</A>.
 *
 * @author Gabor Cselle
 * @version 1.0
 */
public class PersistentQueueImplSingleFile implements PersistentQueue {
    private final String filename;
    private final int defragmentInterval;
    /** How many remove()s have we executed since last defragmenting the file? */
    private int removesSinceDefragment = 0;

    /** Default number of remove() operations between writing defragmented list files. */
    private final static int DEFAULT_DEFRAGMENT_INTERVAL = 50;

    private long maxSizeInByte;
    /**
     * What to postfix the given filename with to get the filename that
     * should be used for the temporary file.
     */
    private final static String TEMPFILE_NAME_POSTFIX = ".temp";

    private LinkedList<Serializable> list;

    /**
     * Create a persistent queue.
     * @param filename the file to use for keeping the persistent state
     * @throws IOException if an I/O error occurs
     */
    public PersistentQueueImplSingleFile(String filename, long maxSizeInMB) throws IOException {
        this(filename, DEFAULT_DEFRAGMENT_INTERVAL, maxSizeInMB);
    }

    /**
     * Create a persistent queue.
     * The state of the queue should be kept in a file with <code>filename</code>.
     * @param filename filename the file to use for keeping the persistent state
     * @param defragmentInterval number of deletes after which defragment operation should start
     * @throws IOException if an I/O error occurs
     */
    public PersistentQueueImplSingleFile(String filename, int defragmentInterval, long maxSizeInMB) throws IOException {
        this.filename = filename;
        this.defragmentInterval = defragmentInterval;
        this.removesSinceDefragment = 0;
        this.maxSizeInByte = maxSizeInMB * 1024 * 1024;

        list = new LinkedList<Serializable>();
        File file = new File(filename);

        // if file does exists:
        if (file.exists()) {
            // read in the file contents
            readStateFromFile(this.filename);
        } else {
            // else, if file exists
            createEmptyFile(this.filename);
        }
    }

    /**
     * Returns true if the queue contains no elements.
     * @return true if the queue contains no elements.
     */
    public synchronized boolean isEmpty() {
        return list.size() == 0;
    }

    /**
     * Returns the number of elements in this queue.
     * @return the number of elements in this queue
     */
    public synchronized long size() {
        return list.size();
    }

    @Override
    public long getUsedDiskSpaceInByte() {
        return (new File(this.filename).length());
    }

    /**
     * Retrieves, but does not remove, the head of this queue,
     * returning <code>null</code> if this queue is empty.
     * @return the head of this queue, or null if this queue is empty.
     */
    public synchronized Serializable peek() {
        if (list.size() != 0)
            return list.get(0);

        return null;
    }

    /**
     * Removes and returns the head element of the persistent queue.
     * @return head element of this queue, or <code>null</code> if queue is empty.
     * @throws IOException if an I/O error occurs
     */
    public synchronized Serializable poll() throws IOException {
        if (list.size() == 0) {
            return null;
        }

        Serializable entry = list.remove(0);

        // defragment file if needed
        removesSinceDefragment++;
        if (removesSinceDefragment >= defragmentInterval) {
            defragmentFile();
            removesSinceDefragment = 0;
        } else {
            // or just append to the file
            appendEntryToFile(filename, new PersistentQueueDeleteMarker());
        }

        return entry;
    }

    /**
     * Adds an element to the tail of the queue.
     * @param element the element to add
     * @throws IOException if an I/O error occurs
     */
    public synchronized boolean add(Serializable element) throws NotEnoughMemoryException, LongOverflowException {
        try {
            assertEnoughMemory();
            appendEntryToFile(filename, element);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return false;
        }

        list.add(element);

        return true;
    }

    private void assertEnoughMemory() throws NotEnoughMemoryException {
        if (new File(this.filename).length() >= maxSizeInByte) throw new NotEnoughMemoryException();
    }

    /** Creates an empty file with no content (0 bytes size) with given filename. */
    private void createEmptyFile(String filename) throws IOException {
        File emptyFile = new File(filename);
        if (!emptyFile.createNewFile()) {
            throw new IOException("Could not create new file: " + filename);
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * Completely re-read all elements from the file with given filename.
     * Clears any elements that are still in queue when method is called.
     * */
    private synchronized void readStateFromFile(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(filename);

        // clear the list - we're reading it afresh from the file
        list.clear();

        // loop over all data that is in that file
        while (fis.available() > 0) {
            Serializable pqentry;

            // a new object input stream to read in the next object
            ObjectInputStream ois = new ObjectInputStream(fis);
            try {
                pqentry = (Serializable)ois.readObject();
            } catch (ClassNotFoundException e) {
                // convert to a IOException
                throw new IOException(e.toString());
            } catch (ClassCastException e) {
                // convert to a IOException
                throw new IOException(e.toString());
            } catch (StreamCorruptedException e) {
                throw new IOException(e.toString());
            }

            // now adjust our internal data structures by adding this object:
            if (pqentry instanceof PersistentQueueDeleteMarker) {
                list.remove(0);
            } else {
                try {
                    list.add((Serializable)pqentry);
                } catch (ClassCastException e) {
                    // convert to a IOException
                    throw new IOException(e.toString());
                }
            }
        }

        fis.close();
    }

    /** Attaches an entry to the file with given filename.*/
    private synchronized void appendEntryToFile(String filename, Serializable entry)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(filename, true);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(entry);

        oos.flush(); oos.close();
        fos.flush(); fos.close();
    }

    /** Writes the current list to a file with given filename */
    private synchronized void writeListFile(String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);

        if (list.isEmpty()) {
            fos.flush(); fos.close();
            return;
        }

        ObjectOutputStream oos = null;
        for (Serializable entry: list) {
            oos = new ObjectOutputStream(fos);
            oos.writeObject(entry);
        }

        oos.flush(); oos.close();
        fos.flush(); fos.close();
    }

    /** Writes defragmented file and renames it to the original filename. */
    private synchronized void defragmentFile() throws IOException {
        String defragmentedFileName = filename + TEMPFILE_NAME_POSTFIX;

        // write out defragmented file
        writeListFile(defragmentedFileName);

        File defragmentedFile = new File(defragmentedFileName);
        File originalFile = new File(filename);

        // rename the defragmented file to the original file name
        // this is the only critical operation where we can loose all data
        // if we crash in the middle of it
        originalFile.delete();
        if (!defragmentedFile.renameTo(originalFile)) {
            throw new IOException("Unable to rename " + defragmentedFileName +
                    " to " + filename);
        }
    }
}