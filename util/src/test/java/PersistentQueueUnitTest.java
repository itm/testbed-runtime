import de.uniluebeck.itm.tr.util.persistentQueue.LongOverflowException;
import de.uniluebeck.itm.tr.util.persistentQueue.NotEnoughMemoryException;
import de.uniluebeck.itm.tr.util.persistentQueue.PersistentQueue;
import de.uniluebeck.itm.tr.util.persistentQueue.impl.PersistentQueueImplMultiFile;
import de.uniluebeck.itm.tr.util.persistentQueue.impl.PersistentQueueImplSingleFile;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.Assert.*;


public abstract class PersistentQueueUnitTest{

    private List<String> elements;
    private PersistentQueue queue;

    public PersistentQueueUnitTest(PersistentQueue queue){
        this.queue = queue;
        this.elements = new LinkedList<String>();

        for (int i=0; i< 50; i++){
            elements.add(new String("element " + i));
        }

    }

    @Test
    public void testNewQueueIsEmpty() {
        if (queue.isEmpty()){
            assertTrue(queue.isEmpty());
            assertEquals(queue.size(), 0);
        }
    }

    @Test
    public void testAddToQueue() throws NotEnoughMemoryException, LongOverflowException {
        int numberOfInserts = 6;
        long sizeBefore = queue.size();
        for (int i = 0; i < numberOfInserts; i++) {
            queue.add("zzz");
        }
        assertTrue(!queue.isEmpty());
        assertEquals(queue.size(), sizeBefore + numberOfInserts);
    }

    @Test
    public void testAddThenPoll() throws NotEnoughMemoryException, LongOverflowException, ClassNotFoundException, IOException {
        String message = "hello";
        assertTrue(queue.add(message));
        while(queue.size() > 1){
            assertNotNull(queue.poll());
        }
        assertEquals(queue.poll(), message);
    }

    @Test
    public void testAddThenPeek() throws ClassNotFoundException, IOException, NotEnoughMemoryException, LongOverflowException {
        String message = "hello";
        queue.add(message);
        long size = queue.size();
        assertEquals(queue.peek(), message);
        assertEquals(queue.size(), size);
    }

    @Test
    public void testFiftyInThenFiftyOut() throws NotEnoughMemoryException, LongOverflowException, ClassNotFoundException, IOException {
        while(!queue.isEmpty()){
            assertNotNull(queue.poll());
        }

        for (int i = 0; i < 50; i++) {
            queue.add(i);
        }
        for (int i = 0; i < 50; i++) {
            assertEquals(queue.poll(), i);
        }
    }

    @Test
    public void testRemovingDownToEmpty() throws NotEnoughMemoryException, LongOverflowException, ClassNotFoundException, IOException {
        int numberOfRemoves = (int)(Math.random() * 20 + 1);
        for (int i = 0; i < numberOfRemoves; i++) {
            queue.add("zzz");
        }
        for (int i = 0; i < numberOfRemoves; i++) {
            queue.poll();
        }
        assertTrue(queue.isEmpty());
        assertEquals(queue.size(), 0);
    }

    @Test
    public void testRemoveOnEmptyQueue() throws ClassNotFoundException, IOException {
        while (!queue.isEmpty()){
            queue.poll();
        }
        assertTrue(queue.isEmpty());
        assertNull(queue.poll());
    }

    @Test
    public void testPeekIntoEmptyQueue() throws ClassNotFoundException, IOException {
        while(!queue.isEmpty()){
            queue.poll();
        }
        assertTrue(queue.isEmpty());
        assertNull(queue.peek());
    }

    @Test
    public void randomizedAddAndPoll() throws ClassNotFoundException, IOException, NotEnoughMemoryException, LongOverflowException {
        while(!queue.isEmpty()){
            queue.poll();
        }

        for (int i=0;i<6;i++){
            assertTrue(queue.add(i));
        }
        assertEquals(queue.poll(), 0);
        assertEquals(queue.poll(), 1);
        assertTrue(queue.add(10));
        assertTrue(queue.add(12));
        assertEquals(queue.poll(), 2);
        assertEquals(queue.poll(), 3);
        assertEquals(queue.poll(), 4);
        assertTrue(queue.add(13));
        assertEquals(queue.poll(), 5);
        assertEquals(queue.poll(), 10);
        assertEquals(queue.poll(), 12);
        assertEquals(queue.poll(), 13);
        assertTrue(queue.isEmpty());
    }

    private class AddToQueueRunnable implements Runnable {

        private PersistentQueue queue;
        private Serializable o;
        public AddToQueueRunnable(PersistentQueue queue, Serializable o){this.queue = queue; this.o = o;}

        @Override
        public void run() {
            try {
                queue.add(o);
            } catch (NotEnoughMemoryException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (LongOverflowException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private class PollFromQueueRunnable implements Runnable {

        private PersistentQueue queue;
        public PollFromQueueRunnable(PersistentQueue queue){this.queue = queue;}

        @Override
        public void run() {
            try {
                assertNotNull(queue.poll());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    @Test
    public void multiThreadedAddAndPoll() throws Exception, IOException {
        while(!queue.isEmpty()){
            queue.poll();
        }
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        int cnt = 100;
        List<Future> futures = new LinkedList<Future>();
        for (int i=0;i<cnt;i++){
            futures.add(executorService.submit(new AddToQueueRunnable(queue, i)));
        }
        
        for (Future future : futures){
            Object futureObj = future.get();
            if (futureObj instanceof Exception) {
                throw (Exception) futureObj;
            } else {
                assertSame(null, future.get());
            }
        }

        for (int i=0;i<cnt;i++){
            futures.add(executorService.submit(new PollFromQueueRunnable(queue)));
        }
        
        for (Future future : futures){
            Object futureObj = future.get();
            if (futureObj instanceof Exception) {
                throw (Exception) futureObj;
            } else {
                assertSame(null, future.get());
            }
        }

    }

    /*
     * @Test
     * commented out as it lengthens the build process astronomically
     */
    public void compareOfPersistentQueues() throws ClassNotFoundException, IOException, NotEnoughMemoryException, LongOverflowException {
        PersistentQueue queueSingleFile = new PersistentQueueImplSingleFile("PersistentQueueUnitTestSingleFileCompare", 50);
        PersistentQueue queueMultiFile = new PersistentQueueImplMultiFile("PersistentQueueUnitTestMultiFileCompare", 50);

        int elements = 1000;
        //Adding to queue
        System.out.print("\nDuration time for " + elements + " elements:\n\nAdding to queue:\nSingleFileQueue: ");
        long startedTimeInMillis = System.currentTimeMillis();
        for (int i=0; i<elements; i++){
            queueSingleFile.add("element " + i);
        }
        System.out.print(System.currentTimeMillis() - startedTimeInMillis + " millis. \tMultiFileQueue: ");
        startedTimeInMillis = System.currentTimeMillis();
        for (int i=0; i<elements; i++){
            queueMultiFile.add("element " + i);
        }
        long discSpaceQueueSingleFile = queueSingleFile.getUsedDiskSpaceInByte();
        long discSpaceQueueMultiFile = queueMultiFile.getUsedDiskSpaceInByte();
        System.out.print(System.currentTimeMillis() - startedTimeInMillis + " millis.");
        System.out.print("\n\nRemoving from queue:\nSingleFileQueue: ");
        //Removing
        startedTimeInMillis = System.currentTimeMillis();
        while (!queueSingleFile.isEmpty()){
            queueSingleFile.poll();
        }
        System.out.print(System.currentTimeMillis() - startedTimeInMillis + " millis. \tMultiFileQueue: ");
        startedTimeInMillis = System.currentTimeMillis();
        for (int i=0; i<elements; i++){
            queueMultiFile.poll();
        }
        System.out.print(System.currentTimeMillis() - startedTimeInMillis + " millis.");
        System.out.print("\n\nUsed Disk Space:\n\nSingleFileQueue: " + discSpaceQueueSingleFile + " byte.\tMultiFileQueue: " + discSpaceQueueMultiFile + " byte.");        
    }
}
