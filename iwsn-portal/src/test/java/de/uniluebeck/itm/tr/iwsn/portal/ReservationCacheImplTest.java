package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReservationCacheImplTest extends ReservationTestBase {

    @Mock
    private SchedulerServiceFactory schedulerServiceFactory;

    @Mock
    private SchedulerService schedulerService;

    private ReservationCacheImpl cache;

    @Before
    public void setUp() throws Exception {
        when(schedulerServiceFactory.create(anyInt(), anyString())).thenReturn(schedulerService);
        cache = new ReservationCacheImpl(schedulerServiceFactory);
    }
}
