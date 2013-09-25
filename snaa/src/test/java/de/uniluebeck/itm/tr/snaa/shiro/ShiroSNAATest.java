package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.AbstractModule;
import de.uniluebeck.itm.tr.snaa.shiro.dao.UrnResourceGroupDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.UserDao;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.persistence.EntityManager;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertFalse;
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

	@Test
	public void testIfAuthorizedWhenDatabaseIsEmpty() throws Exception {
		UsernameNodeUrnsMap map = new UsernameNodeUrnsMap();
		map.setUrnPrefix(NODE_URN_PREFIX_1);
		map.setUsername("NonExistentUser1");
		final AuthorizationResponse authorizationResponse =
				shiroSNAA.isAuthorized(newArrayList(map), Action.RS_DELETE_RESERVATION);
		assertFalse(authorizationResponse.isAuthorized());
	}
}
