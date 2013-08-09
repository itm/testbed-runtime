package de.uniluebeck.itm.tr.snaa.shiro;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Properties;

@RunWith(MockitoJUnitRunner.class)
public class ShiroSNAAMySQLIntegrationTesting extends ShiroSNAATestBase {

	@Before
	public void setUp() throws Exception {

		final Properties jpaProperties = new Properties();

		//jpaProperties.put("hibernate.bytecode.use_reflection_optimizer", "false");
		jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
		jpaProperties.put("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
		jpaProperties.put("hibernate.connection.url", "jdbc:mysql://localhost:3306/trauthsampledb");
		jpaProperties.put("hibernate.connection.username", "trauthuser");
		jpaProperties.put("hibernate.connection.password", "trauthuser");

		super.setUp(new JpaModule("ShiroSNAATest", jpaProperties));
	}
}
