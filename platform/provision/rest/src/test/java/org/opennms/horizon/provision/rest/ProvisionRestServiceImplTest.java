package org.opennms.horizon.provision.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;
import javax.persistence.EntityExistsException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.netmgt.provision.LocationAwareDetectorClient;
import org.opennms.netmgt.provision.persistence.dto.ForeignSourceDTO;
import org.opennms.netmgt.provision.persistence.dto.RequisitionDTO;
import org.opennms.netmgt.provision.service.Provisioner;

public class ProvisionRestServiceImplTest {
    @Mock
    Provisioner provisioner;

    @Mock
    LocationAwareDetectorClient locationAwareDetectorClient;

    SessionUtils sessionUtils;

    ProvisionRestService provisionRestService;
    String requisitionJsonStr;
    RequisitionDTO requisitionDTO;
    Gson gson = new Gson();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        sessionUtils = new MockSessionUtils();
        provisionRestService = new ProvisionRestServiceImpl(provisioner, sessionUtils, locationAwareDetectorClient);

        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("import_dummy-empty.json");
        if (stream == null) {
            throw new FileNotFoundException("Test file does not exist.");
        }
        requisitionJsonStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        requisitionDTO = gson.fromJson(requisitionJsonStr, RequisitionDTO.class);
    }

    @Test
    public void publishRequisition() throws Exception{
        when(provisioner.publish((RequisitionDTO) any())).thenReturn("blahId");

        Response response = provisionRestService.publishRequisition(requisitionJsonStr);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        verify(provisioner).publish((RequisitionDTO) any());
        verifyNoMoreInteractions(provisioner);
    }

    @Test
    public void publishForeignSource() throws Exception{
        when(provisioner.publish((ForeignSourceDTO) any())).thenReturn("blahId");

        Response response = provisionRestService.publishForeignSource(requisitionJsonStr);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        verify(provisioner).publish((ForeignSourceDTO) any());
        verifyNoMoreInteractions(provisioner);
    }

    @Test
    public void publishRequisitionError() throws Exception{
        when(provisioner.publish((RequisitionDTO) any())).thenThrow(new EntityExistsException());

        Response response = provisionRestService.publishRequisition(requisitionJsonStr);

        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        verify(provisioner).publish((RequisitionDTO) any());
        verifyNoMoreInteractions(provisioner);
    }

    public void updateRequisition() throws Exception{
        when(provisioner.update(any())).thenReturn("blahId");

        Response response = provisionRestService.updateRequisition(requisitionJsonStr);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        verify(provisioner).update(any());
        verifyNoMoreInteractions(provisioner);
    }

    @Test
    public void updateRequisitionError() throws Exception{
        when(provisioner.update(any())).thenThrow(new EntityExistsException());

        Response response = provisionRestService.updateRequisition(requisitionJsonStr);

        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        verify(provisioner).update(any());
        verifyNoMoreInteractions(provisioner);
    }

    @Test
    public void getRequisition() {
        when(provisioner.read(anyString())).thenReturn(Optional.of(requisitionDTO));
        Response response = provisionRestService.getRequisition("blahId");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(provisioner).read(anyString());
        verifyNoMoreInteractions(provisioner);
    }

    @Test
    public void getRequisitionNotFound() {
        when(provisioner.read(anyString())).thenReturn(Optional.empty());
        Response response = provisionRestService.getRequisition("blahId");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getEntity());
        verify(provisioner).read(anyString());
        verifyNoMoreInteractions(provisioner);
    }

    @Test
    public void deleteRequisition() {
        Response response = provisionRestService.deleteRequisition("blahId");
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        verify(provisioner).delete(anyString());
        verifyNoMoreInteractions(provisioner);
    }

    @Test()
    public void deleteRequisitionError() {
        doThrow(new IllegalArgumentException()).when(provisioner).delete(anyString());
        Response response = provisionRestService.deleteRequisition("blahId");
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(provisioner).delete(anyString());
        verifyNoMoreInteractions(provisioner);
    }

    public class MockSessionUtils implements SessionUtils {
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
