package de.uniluebeck.itm.tr.common.config;

import com.google.inject.TypeLiteral;
import org.nnsoft.guice.rocoto.converters.AbstractConverter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static de.uniluebeck.itm.util.files.FilePreconditions.checkFileReadable;

public class PropertiesTypeConverter extends AbstractConverter<Properties> {

	@Override
	public Object convert(final String filePath, final TypeLiteral<?> toType) {

		if ("".equals(filePath)) {
			return new Properties();
		}

		final File file = checkFileReadable(new File(filePath));
		final Properties properties = new Properties();

		try {

			properties.load(new FileReader(file));
			return properties;

		} catch (IOException e) {
			throw new IllegalArgumentException(
					"The file \"" + file.getAbsolutePath() + "\" is not a valid properties File!"
			);
		}
	}
}
