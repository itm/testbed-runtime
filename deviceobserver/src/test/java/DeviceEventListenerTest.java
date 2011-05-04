import de.uniluebeck.itm.deviceobserver.DeviceEventListener;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotConnectableException;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotDisconnectableException;
import de.uniluebeck.itm.deviceobserver.impl.MockDeviceEventListenerImpl;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 04.05.11
 * Time: 15:17
 * To change this template use File | Settings | File Templates.
 */
public class DeviceEventListenerTest {

	private DeviceEventListener deviceEventListener;

	@Before
	public void setUp(){
		this.deviceEventListener = new MockDeviceEventListenerImpl();
	}

	@Test
	public void testConnectedAndDisconnected() {
		try {
			this.deviceEventListener.connected(CsvDeviceTestOutput.pacemateMap);
		} catch (DeviceNotConnectableException e) {
			fail("Should have not raised a DeviceNotConnectableException for device: " + CsvDeviceTestOutput.pacemateMap);
		}
		try {
			this.deviceEventListener.disconnected(CsvDeviceTestOutput.pacemateMap);
		} catch (DeviceNotDisconnectableException e){
			fail("Should have not raised a DeviceNotConnectableException");
		}
	}

	@Test
	public void testThrowExceptions(){
		try {
			this.deviceEventListener.connected(CsvDeviceTestOutput.notConnectableMap);
			fail("Should have raised a DeviceNotConnectableException");
		} catch (DeviceNotConnectableException e) {

		}

		try {
			this.deviceEventListener.disconnected(CsvDeviceTestOutput.notConnectableMap);
			fail("Sould have raised a DeviceNotDisconnectableException");
		} catch (DeviceNotDisconnectableException e){

		}
	}
}
