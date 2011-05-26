package de.uniluebeck.itm.motelist;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceInfo {

	public final String reference;

	public final String port;

	public final String type;

	public DeviceInfo(final String port, final String reference, final String type) {

		checkNotNull(port);
		checkNotNull(reference);
		checkNotNull(type);

		this.port = port;
		this.reference = reference;
		this.type = type;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final DeviceInfo that = (DeviceInfo) o;

		if (!port.equals(that.port)) {
			return false;
		}
		if (!reference.equals(that.reference)) {
			return false;
		}
		if (!type.equals(that.type)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = reference.hashCode();
		result = 31 * result + port.hashCode();
		result = 31 * result + type.hashCode();
		return result;
	}
}