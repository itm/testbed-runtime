package de.uniluebeck.itm.tr.common.config;

import com.google.common.net.HostAndPort;
import com.google.inject.TypeLiteral;
import org.nnsoft.guice.rocoto.converters.AbstractConverter;

public class HostAndPortTypeConverter extends AbstractConverter<HostAndPort> {

	@Override
	public Object convert(final String value, final TypeLiteral<?> toType) {
		return value == null || "".equals(value) ? null : HostAndPort.fromString(value);
	}
}
