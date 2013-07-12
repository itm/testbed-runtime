package de.uniluebeck.itm.tr.common.config;

import com.google.inject.TypeLiteral;
import org.nnsoft.guice.rocoto.converters.AbstractConverter;

import java.net.URI;

public class URITypeConverter extends AbstractConverter<URI> {

	@Override
	public Object convert(final String value, final TypeLiteral<?> toType) {
		return URI.create(value);
	}
}
