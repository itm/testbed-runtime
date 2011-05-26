package de.uniluebeck.itm.motelist;


public class DeviceEvent {

	public static enum Type {
		ATTACHED,
		REMOVED
	}

	private final Type type;

	private final DeviceInfo newDeviceInfo;

	public DeviceEvent(final Type type, final DeviceInfo newDeviceInfo) {
		this.type = type;
		this.newDeviceInfo = newDeviceInfo;
	}

	public Type getType() {
		return type;
	}

	public DeviceInfo getNewDeviceInfo() {
		return newDeviceInfo;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final DeviceEvent that = (DeviceEvent) o;

		if (!newDeviceInfo.equals(that.newDeviceInfo)) {
			return false;
		}
		if (type != that.type) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + newDeviceInfo.hashCode();
		return result;
	}
}
