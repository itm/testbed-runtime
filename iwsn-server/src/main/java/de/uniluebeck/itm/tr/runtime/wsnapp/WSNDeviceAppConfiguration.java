package de.uniluebeck.itm.tr.runtime.wsnapp;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class WSNDeviceAppConfiguration {

	private final String nodeUrn;

	@Nullable
	private File defaultImageFile;

	public WSNDeviceAppConfiguration(final String nodeUrn, @Nullable final File defaultImageFile) {

		this.nodeUrn = nodeUrn;

		final boolean defaultImageFileNullOrReadableFile = defaultImageFile == null ||
				(defaultImageFile.exists() && defaultImageFile.isFile() && defaultImageFile.canRead());
		checkArgument(defaultImageFileNullOrReadableFile,
				"The default image for " + nodeUrn +
						" (\"" + (defaultImageFile != null ? defaultImageFile.getAbsolutePath() : "") + "\") "
						+ "either does not exists, is not a file or is not readable!"
		);
		this.defaultImageFile = defaultImageFile;
	}

	public String getNodeUrn() {
		return nodeUrn;
	}

	@Nullable
	public File getDefaultImageFile() {
		return defaultImageFile;
	}
}
