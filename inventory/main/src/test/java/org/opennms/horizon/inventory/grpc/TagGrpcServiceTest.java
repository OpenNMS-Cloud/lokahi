package org.opennms.horizon.inventory.grpc;

import com.google.protobuf.BoolValue;
import com.google.rpc.Code;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.TagService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TagGrpcServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";

    private TagService mockTagService;
    private TenantLookup mockTenantLookup;
    private NodeService mockNodeService;

    private TagCreateDTO testTagCreateDTO;
    private TagCreateListDTO testTagCreateListDTO;
    private ListTagsByEntityIdParamsDTO testListTagsByEntityIdParamsDTO;
    private ListAllTagsParamsDTO testListAllTagsParamsDTO;
    private TagRemoveListDTO testTagRemoveListDTO;
    private DeleteTagsDTO testDeleteTagsDTO;

    private TagGrpcService target;

    @BeforeEach
    public void setUp() {
        mockTagService = Mockito.mock(TagService.class);
        mockTenantLookup = Mockito.mock(TenantLookup.class);
        mockNodeService = Mockito.mock(NodeService.class);

        testTagCreateDTO =
            TagCreateDTO.newBuilder()
                .setName("x-tag-x")
                .build();

        testTagCreateListDTO =
            TagCreateListDTO.newBuilder()
                .addTags(testTagCreateDTO)
                .build();

        testTagRemoveListDTO =
            TagRemoveListDTO.newBuilder()
                .addEntityIds(TagEntityIdDTO.newBuilder().setNodeId(2929).build())
                .build();

        testListTagsByEntityIdParamsDTO =
            ListTagsByEntityIdParamsDTO.newBuilder()
                .setEntityId(TagEntityIdDTO.newBuilder().setNodeId(3131).build())
                .build();

        testListAllTagsParamsDTO =
            ListAllTagsParamsDTO.newBuilder()
                .build();

        testDeleteTagsDTO =
            DeleteTagsDTO.newBuilder()
                .build();

        target = new TagGrpcService(mockTagService, mockTenantLookup, mockNodeService);
    }

    @Test
    void testAddTags() {
        //
        // Setup Test Data and Interactions
        //
        var testTag =
            TagDTO.newBuilder()
                .setName("x-tag-x")
                .build();
        List<TagDTO> testTagList = List.of(testTag);

        prepareCommonTenantLookup();
        StreamObserver<TagListDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockTagService.addTags(TEST_TENANT_ID, testTagCreateListDTO)).thenReturn(testTagList);

        //
        // Execute
        //
        target.addTags(testTagCreateListDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockStreamObserver).onNext(
            Mockito.argThat(
                (argument) -> Objects.equals(testTagList, argument.getTagsList())
            )
        );
        Mockito.verify(mockStreamObserver).onCompleted();
    }

    @Test
    void testAddTagsException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<TagListDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockTagService.addTags(TEST_TENANT_ID, testTagCreateListDTO)).thenThrow(testException);

        //
        // Execute
        //
        target.addTags(testTagCreateListDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INTERNAL_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testAddTagsMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<TagListDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.addTags(testTagCreateListDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testRemoveTags() {
        //
        // Setup Test Data and Interactions
        //
        var testTag =
            TagDTO.newBuilder()
                .setName("x-tag-x")
                .build();
        List<TagDTO> testTagList = List.of(testTag);

        prepareCommonTenantLookup();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockTagService.addTags(TEST_TENANT_ID, testTagCreateListDTO)).thenReturn(testTagList);

        //
        // Execute
        //
        target.removeTags(testTagRemoveListDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockStreamObserver).onNext(BoolValue.of(true));
        Mockito.verify(mockStreamObserver).onCompleted();
        Mockito.verify(mockTagService).removeTags(TEST_TENANT_ID, testTagRemoveListDTO);
    }

    @Test
    void testRemoveTagsException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.doThrow(testException).when(mockTagService).removeTags(TEST_TENANT_ID, testTagRemoveListDTO);

        //
        // Execute
        //
        target.removeTags(testTagRemoveListDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INTERNAL_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testRemoveTagsMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.removeTags(testTagRemoveListDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetTagsByEntityId() {
        //
        // Setup Test Data and Interactions
        //
        var testTag =
            TagDTO.newBuilder()
                .setName("x-tag-x")
                .build();
        List<TagDTO> testTagList = List.of(testTag);

        prepareCommonTenantLookup();
        StreamObserver<TagListDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockTagService.getTagsByEntityId(TEST_TENANT_ID, testListTagsByEntityIdParamsDTO)).thenReturn(testTagList);

        //
        // Execute
        //
        target.getTagsByEntityId(testListTagsByEntityIdParamsDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockStreamObserver).onNext(
            Mockito.argThat(
                (argument) -> Objects.equals(testTagList, argument.getTagsList())
            )
        );
        Mockito.verify(mockStreamObserver).onCompleted();
    }

    @Test
    void testGetTagsByEntityIdTagsException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<TagListDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockTagService.getTagsByEntityId(TEST_TENANT_ID, testListTagsByEntityIdParamsDTO)).thenThrow(testException);

        //
        // Execute
        //
        target.getTagsByEntityId(testListTagsByEntityIdParamsDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INTERNAL_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetTagsByEntityIdMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<TagListDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.getTagsByEntityId(testListTagsByEntityIdParamsDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetTags() {
        //
        // Setup Test Data and Interactions
        //
        var testTag =
            TagDTO.newBuilder()
                .setName("x-tag-x")
                .build();
        List<TagDTO> testTagList = List.of(testTag);

        prepareCommonTenantLookup();
        StreamObserver<TagListDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockTagService.getTags(TEST_TENANT_ID, testListAllTagsParamsDTO)).thenReturn(testTagList);

        //
        // Execute
        //
        target.getTags(testListAllTagsParamsDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockStreamObserver).onNext(
            Mockito.argThat(
                (argument) -> Objects.equals(testTagList, argument.getTagsList())
            )
        );
        Mockito.verify(mockStreamObserver).onCompleted();
    }

    @Test
    void testGetTagsException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<TagListDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockTagService.getTags(TEST_TENANT_ID, testListAllTagsParamsDTO)).thenThrow(testException);

        //
        // Execute
        //
        target.getTags(testListAllTagsParamsDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INTERNAL_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetTagsMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<TagListDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.getTags(testListAllTagsParamsDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testDeleteTags() {
        //
        // Setup Test Data and Interactions
        //
        var testTag =
            TagDTO.newBuilder()
                .setName("x-tag-x")
                .build();
        List<TagDTO> testTagList = List.of(testTag);

        prepareCommonTenantLookup();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.deleteTags(testDeleteTagsDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockStreamObserver).onNext(BoolValue.of(true));
        Mockito.verify(mockStreamObserver).onCompleted();
        Mockito.verify(mockTagService).deleteTags(TEST_TENANT_ID, testDeleteTagsDTO);
    }

    @Test
    void testDeleteTagsException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.doThrow(testException).when(mockTagService).deleteTags(TEST_TENANT_ID, testDeleteTagsDTO);

        //
        // Execute
        //
        target.deleteTags(testDeleteTagsDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INTERNAL_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testDeleteTagsMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.deleteTags(testDeleteTagsDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }


//========================================
// Internals
//----------------------------------------

    private void prepareCommonTenantLookup() {
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class))).thenReturn(Optional.of(TEST_TENANT_ID));
    }

    private void prepareTenantLookupOnMissingTenant() {
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class))).thenReturn(Optional.empty());
    }

    private ArgumentMatcher<Exception> prepareStatusExceptionMatcher(int expectedCode, String expectedMessage) {
        return argument ->
            (
                (argument instanceof StatusRuntimeException) &&
                (((StatusRuntimeException) argument).getStatus().getCode().value() == expectedCode)  &&
                argument.getMessage().contains(expectedMessage)
            );
    }
}
