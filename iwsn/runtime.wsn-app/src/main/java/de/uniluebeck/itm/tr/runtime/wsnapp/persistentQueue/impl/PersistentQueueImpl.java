package de.uniluebeck.itm.tr.runtime.wsnapp.persistentQueue.impl;

import java.io.*;

import de.uniluebeck.itm.tr.runtime.wsnapp.persistentQueue.LongOverflowException;
import de.uniluebeck.itm.tr.runtime.wsnapp.persistentQueue.NotEnoughMemoryException;
import de.uniluebeck.itm.tr.runtime.wsnapp.persistentQueue.PersistentQueue;
import de.uniluebeck.itm.tr.util.FileUtils;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 10.08.2010
 * Time: 16:45:45
 * To change this template use File | Settings | File Templates.
 */
public class PersistentQueueImpl implements PersistentQueue {

    private File dir;

    //TODO check overrun
    private long size = 0;
    private long head = 0;
    private long next = 0;

    private final static String prefix = "pq_";
    private final static String suffix = ".tmp";

    private long maxSizeInMegaByte;

    private long clusterSize = 4096;

    private long sizeOfDirectoryInByte = 0;
    
    final org.slf4j.Logger log = LoggerFactory.getLogger(PersistentQueueImpl.class);
    
    public PersistentQueueImpl(String dir, long maxSizeInMegaByte) throws IOException {
        this.dir = new File(dir);
        this.maxSizeInMegaByte = maxSizeInMegaByte;

        this.createAndRemoveDir();
    }

    public synchronized boolean add(Serializable object) throws NotEnoughMemoryException, LongOverflowException {
        try {
            assertMaxSizeReached();
            File file = this.createNewFile();
            this.addObjectToFile(file.getAbsoluteFile(), object);

            this.increaseSizeOfDirectory(file);
            this.size ++; this.next ++;

            return true;
        }
        catch (IOException e){
            return false;
        }
    }

    //increase DirectorySize
    private void increaseSizeOfDirectory(File file) throws LongOverflowException {
        long multiplier = (file.length() / this.clusterSize);
        if (file.length() % this.clusterSize != 0) multiplier++;

        if (Long.MAX_VALUE <= this.sizeOfDirectoryInByte + (this.clusterSize * multiplier))
            throw new LongOverflowException();
        this.sizeOfDirectoryInByte += this.clusterSize * multiplier;
    }

    public synchronized Serializable poll() throws IOException, ClassNotFoundException {
        Serializable object = this.peek();
        File file = this.getFileFromHead();
        file.delete();

        this.head ++;
        this.size --;
        
        return object;
    }

    public synchronized Serializable peek() throws IOException, ClassNotFoundException {
        if (this.isEmpty()){
            log.warn("Queue is empty.");
            return null;
        }
        File file = this.getFileFromHead();
        Serializable object = null;
        FileInputStream fileInputStream = new FileInputStream(file.getAbsoluteFile());
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

        object = (Serializable) objectInputStream.readObject();

        return object;
    }

    public synchronized boolean isEmpty() {
        return this.size == 0;
    }

    public synchronized long size() {
        return this.size;
    }

    private synchronized void addObjectToFile(File file, Serializable object) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

        objectOutputStream.writeObject(object);

        objectOutputStream.flush(); objectOutputStream.close();
        fileOutputStream.flush(); fileOutputStream.close();
    }


    
    private synchronized File getFileFromHead(){
        return new File(this.dir.getAbsolutePath(), prefix + this.head + suffix);
    }

    private void createAndRemoveDir() throws IOException {
        if (this.dir.exists()){
            FileUtils.deleteDirectory(this.dir);
        }
        assertDirExist();

        this.dir.mkdir();
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

    private synchronized File createNewFile() throws IOException {
        long value = this.next;
        File file = new File(this.dir.getAbsolutePath(), prefix + value + suffix);
        if (!file.createNewFile()) throw new IOException("Could not create File");

        file.deleteOnExit();
        if (head == -1) head = value;
        return file;
    }
}
