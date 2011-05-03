import de.uniluebeck.itm.deviceobserver.util.DeviceObserverUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 14.04.11
 * Time: 17:54
 * To change this template use File | Settings | File Templates.
 */
public class CsvDeviceTestOutput {

	private static String VALUE_REFERENCE_ID = "0001";
	private static String VALUE_DEVICE_PORT_USB0 = "/dev/ttyUSB0";
	private static String VALUE_MOCK_DEVICE_DEVICE = "Mock";
	private static String VALUE_NONE_DEVICE_DEVICE = "(none)";

	public static String[] pacemate = new String[]{
			VALUE_REFERENCE_ID + "," + VALUE_DEVICE_PORT_USB0 + "," + VALUE_MOCK_DEVICE_DEVICE,
			VALUE_REFERENCE_ID + ",/dev/ttyUSB1," + VALUE_MOCK_DEVICE_DEVICE,
			VALUE_REFERENCE_ID + ",/dev/ttyUSB2," + VALUE_MOCK_DEVICE_DEVICE
	};
	public static String[] notConnectableDevice = new String[]{
			VALUE_REFERENCE_ID + "," + VALUE_DEVICE_PORT_USB0 + "," + VALUE_NONE_DEVICE_DEVICE
	};

	public static Map<String, String> notConnectableMap = new HashMap<String, String>() {{
		put(DeviceObserverUtils.KEY_REFERENCE_ID, VALUE_REFERENCE_ID);
		put(DeviceObserverUtils.KEY_DEVICE_PORT, VALUE_DEVICE_PORT_USB0);
		put(DeviceObserverUtils.KEY_DEVICE_TYPE, VALUE_NONE_DEVICE_DEVICE);
	}};
}
