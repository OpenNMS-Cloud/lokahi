package org.opennms.horizon.datachoices.internal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.db.dao.api.DataChoicesDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsDataChoices;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StateManagerTest {

    @InjectMocks
    private StateManager stateManager;

    @Mock
    private DataChoicesDao dataChoicesDao;

    @Mock
    private StateManager.StateChangeHandler callback;

    private static OnmsDataChoices getOnmsDataChoices() {
        OnmsDataChoices dataChoices = new OnmsDataChoices();
        dataChoices.setId(1);
        dataChoices.setEnabled(false);
        dataChoices.setSystemId(UUID.randomUUID().toString());
        return dataChoices;
    }

    @Before
    public void setup() {
        stateManager.setSessionUtils(new TestNoOpSessionUtils());
        stateManager.onIsEnabledChanged(callback);
    }

    @Test
    public void testSetEnabled() {
        OnmsDataChoices dataChoices = getOnmsDataChoices();
        when(dataChoicesDao.find()).thenReturn(dataChoices);

        stateManager.setEnabled(true);

        verify(callback, times(1))
            .onEnabledChanged(true);

        assertTrue(dataChoices.getEnabled());
    }

    private static class TestNoOpSessionUtils implements SessionUtils {

        @Override
        public <V> V withTransaction(Supplier<V> supplier) {
            return supplier.get();
        }

        @Override
        public <V> V withReadOnlyTransaction(Supplier<V> supplier) {
            return supplier.get();
        }
    }
}
