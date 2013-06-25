package de.uniluebeck.itm.tr.common.config;

import com.google.inject.TypeLiteral;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.nnsoft.guice.rocoto.converters.AbstractConverter;

public class NodeUrnPrefixTypeConverter extends AbstractConverter<NodeUrnPrefix> {

	@Override
	public Object convert(final String value, final TypeLiteral<?> toType) {
		return new NodeUrnPrefix(value);
	}
}
