package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.AbstractModule;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroup;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShiroSNAATest extends ShiroSNAATestBase {

	static {
		Logging.setLoggingDefaults(LogLevel.ERROR);
	}

	@Mock
	private EntityManager em;

	@Mock
	private CriteriaBuilder criteriaBuilder;

	@Mock
	private CriteriaQuery<UrnResourceGroup> criteriaQuery;

	@Mock
	private TypedQuery<UrnResourceGroup> typedQuery;

	@Before
	public void setUp() throws Exception {

		when(em.find(User.class, EXPERIMENTER1)).thenReturn(getExperimenter1());
		when(em.find(User.class, SERVICE_PROVIDER1)).thenReturn(getServiceProvider1());
		when(em.find(User.class, ADMINISTRATOR1)).thenReturn(getAdministrator1());

		when(em.getCriteriaBuilder()).thenReturn(criteriaBuilder);
		when(criteriaBuilder.createQuery(UrnResourceGroup.class)).thenReturn(criteriaQuery);
		when(em.createQuery(criteriaQuery)).thenReturn(typedQuery);
		when(typedQuery.getResultList()).thenReturn(getUrnResourceGroup());

		final AbstractModule jpaModule = new AbstractModule() {
			@Override
			protected void configure() {
				bind(EntityManager.class).toInstance(em);
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
