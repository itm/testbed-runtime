package de.uniluebeck.itm.tr.common;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static de.uniluebeck.itm.tr.common.Constants.*;

public class ApplicationPropertiesModule extends AbstractModule {

	private static final Logger log = LoggerFactory.getLogger(ApplicationPropertiesModule.class);

	@Override
	protected void configure() {

		ClassPathFactory factory = new ClassPathFactory();
		ClassPath classPath = factory.createFromJVM();

		if (classPath.isResource("de/uniluebeck/itm/tr/common/application.properties")) {

			try {

				Properties p = new Properties();
				p.load(classPath.getResourceAsStream("de/uniluebeck/itm/tr/common/application.properties"));

				bindConstant().annotatedWith(Names.named(APP_NAME_KEY)).to(p.getProperty(APP_NAME_KEY));
				bindConstant().annotatedWith(Names.named(APP_VERSION_KEY)).to(p.getProperty(APP_VERSION_KEY));
				bindConstant().annotatedWith(Names.named(APP_BUILD_KEY)).to(p.getProperty(APP_BUILD_KEY));
				bindConstant().annotatedWith(Names.named(APP_BRANCH_KEY)).to(p.getProperty(APP_BRANCH_KEY));

			} catch (Exception e) {

				log.warn("Exception loading application.properties from classpath: ", e);

				bindConstant().annotatedWith(Names.named(APP_NAME_KEY)).to("unknown");
				bindConstant().annotatedWith(Names.named(APP_VERSION_KEY)).to("unknown");
				bindConstant().annotatedWith(Names.named(APP_BUILD_KEY)).to("unknown");
				bindConstant().annotatedWith(Names.named(APP_BRANCH_KEY)).to("unknown");
			}

		} else {

			log.warn("application.properties not found on classpath!");

			bindConstant().annotatedWith(Names.named(APP_NAME_KEY)).to("unknown");
			bindConstant().annotatedWith(Names.named(APP_VERSION_KEY)).to("unknown");
			bindConstant().annotatedWith(Names.named(APP_BUILD_KEY)).to("unknown");
			bindConstant().annotatedWith(Names.named(APP_BRANCH_KEY)).to("unknown");
		}
	}
}
