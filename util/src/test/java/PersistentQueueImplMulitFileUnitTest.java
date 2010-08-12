import de.uniluebeck.itm.tr.util.persistentQueue.PersistentQueue;
import de.uniluebeck.itm.tr.util.persistentQueue.impl.PersistentQueueImplMultiFile;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 11.08.2010
 * Time: 16:26:49
 * To change this template use File | Settings | File Templates.
 */
public class PersistentQueueImplMulitFileUnitTest extends PersistentQueueUnitTest{

    private PersistentQueue queue;
    public PersistentQueueImplMulitFileUnitTest() throws IOException {
        super(new PersistentQueueImplMultiFile("PersistentQueueUnitTestMultiFile", 12));
    }

}
