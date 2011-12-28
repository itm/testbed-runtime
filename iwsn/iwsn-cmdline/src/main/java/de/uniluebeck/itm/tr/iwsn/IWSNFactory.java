package de.uniluebeck.itm.tr.iwsn;

import java.io.File;

public interface IWSNFactory {

	IWSN create(final File configurationFile, final String nodeId);

}
