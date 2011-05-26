import de.uniluebeck.itm.deviceobserver.DeviceEventListener;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotConnectableException;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotDisconnectableException;
import de.uniluebeck.itm.deviceobserver.impl.MockDeviceEventListenerImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;


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
