package de.uniluebeck.itm.tr.runtime.wsnapp.persistentQueue;


import de.uniluebeck.itm.tr.runtime.wsnapp.persistentQueue.impl.PersistentQueueImpl;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 10.08.2010
 * Time: 17:04:17
 * To change this template use File | Settings | File Templates.
 */
public class Test {
    public static void main(String[] args) throws IOException, NotEnoughMemoryException, ClassNotFoundException, LongOverflowException {
        PersistentQueueImpl queue = new PersistentQueueImpl("test", 1);
        /*
        queue.add("test1");
        queue.add("test2");
        System.out.println(queue.peek());
        System.out.println(queue.poll());
        System.out.println(queue.peek());
        queue.add("test3");
        System.out.println(queue.peek());
        System.out.println(queue.poll());
        System.out.println(queue.peek());
        System.out.println(queue.poll());
        System.out.println(queue.peek());
        System.out.println(queue.poll());
        */

        ///*
        for (int i=0;i<250;i++){
            queue.add("test" + i);
        }

        queue.add("new element");
        System.out.println("added...");

        while(!queue.isEmpty()){
            //System.out.println(queue.poll());
            queue.poll();
        }

        System.out.println("removed...");
        //*/
    }
}
