import de.uniluebeck.itm.tr.util.persistentQueue.LongOverflowException;
import de.uniluebeck.itm.tr.util.persistentQueue.NotEnoughMemoryException;
import de.uniluebeck.itm.tr.util.persistentQueue.PersistentQueue;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 11.08.2010
 * Time: 15:59:47
 * To change this template use File | Settings | File Templates.
 */

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


}
