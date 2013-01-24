package de.uniluebeck.itm.tr.iwsn.devicedb;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.persist.jpa.JpaPersistModule;

import de.uniluebeck.itm.tr.iwsn.devicedb.dao.DeviceConfigDAO;
import de.uniluebeck.itm.tr.iwsn.devicedb.dao.DeviceConfigDB;

public class DeviceConfigDBModule extends AbstractModule {
	
	private static final Logger log = LoggerFactory.getLogger(DeviceConfigDBModule.class);
	private Properties properties;

	@Override
	protected void configure() {
		install(new JpaPersistModule("default").properties(properties));
		bind(JPAInitializer.class).asEagerSingleton();
		bind(DeviceConfigDB.class).to(DeviceConfigDAO.class).in(Scopes.SINGLETON);
	}
	
	public DeviceConfigDBModule() {
		Properties props = new Properties();
		try {
			props.load(DeviceConfigDBModule.class.getClassLoader().getResourceAsStream("META-INF/hibernate.properties"));
			this.properties = props;
		} catch (Exception e) {
			log.error("Error loading hibernate.properties");
		}
	}
	
	public DeviceConfigDBModule(Properties properties) {
		this.properties = properties;
	}
	
}
