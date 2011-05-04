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

	private static String VALUE_REFERENCE_ID_0001 = "0001";
	private static String VALUE_REFERENCE_ID_0002 = "0002";
	private static String VALUE_REFERENCE_ID_0003 = "0003";
	private static String VALUE_DEVICE_PORT_USB0 = "/dev/ttyUSB0";
	private static String VALUE_DEVICE_PORT_USB1 = "/dev/ttyUSB1";
	private static String VALUE_DEVICE_PORT_USB2 = "/dev/ttyUSB2";
	private static String VALUE_MOCK_DEVICE = "Mock";
	private static String VALUE_NONE_DEVICE = "(doNotConnect)";

	public static String[] pacemate = new String[]{
			VALUE_REFERENCE_ID_0001 + "," + VALUE_DEVICE_PORT_USB0 + "," + VALUE_MOCK_DEVICE,
			VALUE_REFERENCE_ID_0002 + "," + VALUE_DEVICE_PORT_USB1 + "," + VALUE_MOCK_DEVICE,
			VALUE_REFERENCE_ID_0003 + "," + VALUE_DEVICE_PORT_USB2 + "," + VALUE_MOCK_DEVICE
	};
	public static String[] notConnectableDevice = new String[]{
			VALUE_REFERENCE_ID_0001 + "," + VALUE_DEVICE_PORT_USB0 + "," + VALUE_NONE_DEVICE
	};

	public static Map<String, String> pacemateMap = new HashMap<String, String>() {{
		put(DeviceObserverUtils.KEY_REFERENCE_ID, VALUE_REFERENCE_ID_0001);
		put(DeviceObserverUtils.KEY_DEVICE_PORT, VALUE_DEVICE_PORT_USB0);
		put(DeviceObserverUtils.KEY_DEVICE_TYPE, VALUE_MOCK_DEVICE);
	}};

	public static Map<String, String> notConnectableMap = new HashMap<String, String>() {{
		put(DeviceObserverUtils.KEY_REFERENCE_ID, VALUE_REFERENCE_ID_0001);
		put(DeviceObserverUtils.KEY_DEVICE_PORT, VALUE_DEVICE_PORT_USB0);
		put(DeviceObserverUtils.KEY_DEVICE_TYPE, VALUE_NONE_DEVICE);
	}};

}
