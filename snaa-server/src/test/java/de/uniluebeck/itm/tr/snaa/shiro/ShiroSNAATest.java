package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.AbstractModule;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.persistence.EntityManager;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShiroSNAATest extends ShiroSNAATestBase {

	@Mock
	private EntityManager em;

	@Mock
	private UserDao usersDao;

	@Mock
	private UrnResourceGroupDao urnResourceGroupDao;

	@Before
	public void setUp() throws Exception {

		when(usersDao.find(EXPERIMENTER1)).thenReturn(getExperimenter1());
		when(usersDao.find(SERVICE_PROVIDER1)).thenReturn(getServiceProvider1());
		when(usersDao.find(ADMINISTRATOR1)).thenReturn(getAdministrator1());
		when(urnResourceGroupDao.find()).thenReturn(getUrnResourceGroup());

		final AbstractModule jpaModule = new AbstractModule() {
			@Override
			protected void configure() {
				bind(EntityManager.class).toInstance(em);
				bind(UserDao.class).toInstance(usersDao);
				bind(UrnResourceGroupDao.class).toInstance(urnResourceGroupDao);
			}
		};

		super.setUp(jpaModule);
	}
}
