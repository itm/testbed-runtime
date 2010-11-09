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

import java.io.*;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.uniluebeck.itm.tr.util.persistentQueue.LongOverflowException;
import de.uniluebeck.itm.tr.util.persistentQueue.NotEnoughMemoryException;
import de.uniluebeck.itm.tr.util.persistentQueue.PersistentQueue;
import de.uniluebeck.itm.tr.util.FileUtils;
import org.slf4j.LoggerFactory;
import sun.awt.datatransfer.ToolkitThreadBlockedHandler;

public class PersistentQueueImplMultiFile implements PersistentQueue {

    private File dir;

    //TODO check overrun
    private long size = 0;
    private long head = 0;
    private long tail = 0;

    private final static String prefix = "pq_";
    private final static String suffix = ".tmp";

    private long maxSizeInMegaByte;

    private static long clusterSize = 4096;

    private long sizeOfDirectoryInByte = 0;
    
    final org.slf4j.Logger log = LoggerFactory.getLogger(PersistentQueueImplMultiFile.class);
	
    private final Lock lock = new ReentrantLock();
    
    private Random random = new Random();

    public PersistentQueueImplMultiFile(String dir, long maxSizeInMegaByte) throws IOException, SecurityException {
		this.dir = new File(dir + File.separator + "PersistentQueueImplMultiFile" + random.nextLong());
        this.maxSizeInMegaByte = maxSizeInMegaByte;

        this.createAndRemoveDir();
    }

    /**
     * Adds serializable object into queue.
     * returns true
     * @param object
     * @return true if and only if the object was
     *          successfully added; false otherwise
     * @throws NotEnoughMemoryException
     * @throws LongOverflowException
     */
    public boolean add(Serializable object) throws NotEnoughMemoryException, LongOverflowException {
        lock.lock();
        try {
            assertMaxSizeReached();
            File file = this.createNewFile();
            this.addObjectToFile(file.getAbsoluteFile(), object);

            this.increaseSizeOfDirectory(file);
            this.size ++; this.tail++;

            return true;
        }
        catch (IOException e){
            return false;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Updates currently used data-space of directory addicted to cluster-size and file-length.
     * @param file
     * @throws LongOverflowException
     */
    private void increaseSizeOfDirectory(File file) throws LongOverflowException {
        long multiplier = (file.length() / this.clusterSize);
        if (file.length() % this.clusterSize != 0) multiplier++;

        if (Long.MAX_VALUE <= this.sizeOfDirectoryInByte + (this.clusterSize * multiplier))
            throw new LongOverflowException();
        this.sizeOfDirectoryInByte += this.clusterSize * multiplier;
    }

    /**
     * Retrieves and removes the head of this queue, or null if this queue is empty.
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SecurityException
     */
    public Serializable poll() throws IOException, ClassNotFoundException, SecurityException {
        Serializable object = null;
        lock.lock();
        try {
            object = this.getHeadObject();
            File file = this.getFileFromHead();
            file.delete();
            this.head ++;
            this.size --;

            return object;
        } finally {
            lock.unlock();
        }
    }

    public Serializable peek() throws IOException, ClassNotFoundException {
        Serializable object = null;
        lock.lock();
        try {
            object = getHeadObject();
            return object;
        } finally {
            lock.unlock();
        }
    }

    private Serializable getHeadObject() throws ClassNotFoundException, IOException {
        if (this.isEmpty()){
            log.warn("Queue is empty.");
            return null;
        }
        File file = this.getFileFromHead();
        FileInputStream fileInputStream = new FileInputStream(file.getAbsoluteFile());
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

        return (Serializable) objectInputStream.readObject();
    }

    public synchronized boolean isEmpty() {
        return this.size == 0;
    }

    public synchronized long size() {
        return this.size;
    }

    @Override
    public long getUsedDiskSpaceInByte() {
        return this.sizeOfDirectoryInByte;
    }

    private void addObjectToFile(File file, Serializable object) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

        objectOutputStream.writeObject(object);

        objectOutputStream.flush(); objectOutputStream.close();
        fileOutputStream.flush(); fileOutputStream.close();
    }


    
    private File getFileFromHead(){
        return new File(this.dir.getAbsolutePath(), prefix + this.head + suffix);
    }

    private void createAndRemoveDir() throws IOException, SecurityException {
        if (this.dir.exists()){
            FileUtils.deleteDirectory(this.dir);
        }
        assertDirExist();

        this.dir.mkdirs();
        this.dir.deleteOnExit();
    }

    private void assertDirExist() {
        if (this.dir.exists()) throw new RuntimeException("Could not delete directory. Aborted.");
    }

    private void assertMaxSizeReached() throws NotEnoughMemoryException {
        if (getSizeOfDirectoryInMB() >= this.maxSizeInMegaByte) throw new NotEnoughMemoryException();
    }

    private long getSizeOfDirectoryInMB(){
        return (this.sizeOfDirectoryInByte / 1024 / 1024);
    }

    private File createNewFile() throws IOException {
        long value = this.tail;
        File file = new File(this.dir.getAbsolutePath(), prefix + value + suffix);
        if (!file.createNewFile()) throw new IOException("Could not create File");

        file.deleteOnExit();
        if (head == -1) head = value;
        return file;
    }
}
