package de.uniluebeck.itm.tr.iwsn;

import java.io.File;

public interface IWSNFactory {

	IWSN create(final File xmlFile, final String nodeId);

}
