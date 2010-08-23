import de.uniluebeck.itm.tr.util.persistentQueue.impl.PersistentQueueImplSingleFile;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 11.08.2010
 * Time: 16:22:08
 * To change this template use File | Settings | File Templates.
 */
public class PersistentQueueImplSingleFileUnitTest extends PersistentQueueUnitTest{

    public PersistentQueueImplSingleFileUnitTest() throws IOException {
        super(new PersistentQueueImplSingleFile("PersistentQueueUnitTestSingleFile", 12));
    }

}
