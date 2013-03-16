package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.assistedinject.Assisted;

public interface DeviceDBServiceFactory {

	DeviceDBService create(@Assisted("path_rest") String restPath, @Assisted("path_webapp") String webAppPath);

}
