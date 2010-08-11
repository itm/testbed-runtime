import de.uniluebeck.itm.tr.util.persistentQueue.PersistentQueue;
import de.uniluebeck.itm.tr.util.persistentQueue.impl.PersistentQueueImpl;
import org.junit.Before;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 11.08.2010
 * Time: 16:22:08
 * To change this template use File | Settings | File Templates.
 */
public class PersistentQueueImplUnitTest extends PersistentQueueUnitTest{

    public PersistentQueueImplUnitTest() throws IOException {
        super(new PersistentQueueImpl("PersistentQueueUnitTest", 12));
    }

}
