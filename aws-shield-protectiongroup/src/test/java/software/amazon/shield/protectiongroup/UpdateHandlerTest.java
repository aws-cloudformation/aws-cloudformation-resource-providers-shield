package software.amazon.shield.protectiongroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupResponse;
import software.amazon.awssdk.services.shield.model.ListResourcesInProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.ListResourcesInProtectionGroupResponse;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.shield.model.ProtectionGroup;
import software.amazon.awssdk.services.shield.model.TagResourceRequest;
import software.amazon.awssdk.services.shield.model.TagResourceResponse;
import software.amazon.awssdk.services.shield.model.UntagResourceRequest;
import software.amazon.awssdk.services.shield.model.UntagResourceResponse;
import software.amazon.awssdk.services.shield.model.UpdateProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.UpdateProtectionGroupResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.protectiongroup.helper.ProtectionGroupTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private UpdateHandler updateHandler;
    private ResourceModel resourceModel;

    @BeforeEach
    public void setup() {
        this.proxy = mock(AmazonWebServicesClientProxy.class);
        this.logger = mock(Logger.class);

        this.updateHandler = new UpdateHandler(mock(ShieldClient.class));
        this.resourceModel = ProtectionGroupTestData.RESOURCE_MODEL;
    }

    @Test
    public void handleRequest_with_tags_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .previousResourceState(ResourceModel.builder()
                                .protectionGroupId(ProtectionGroupTestData.PROTECTION_GROUP_ID)
                                .protectionGroupArn(ProtectionGroupTestData.PROTECTION_GROUP_ARN)
                                .build())
                        .desiredResourceState(this.resourceModel)
                        .nextToken(ProtectionGroupTestData.NEXT_TOKEN)
                        .build();

        final UpdateProtectionGroupResponse updateProtectionGroupResponse =
                UpdateProtectionGroupResponse.builder()
                        .build();

        doReturn(updateProtectionGroupResponse)
                .when(this.proxy).injectCredentialsAndInvokeV2(any(UpdateProtectionGroupRequest.class), any());

        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder()
                .build();

        doReturn(tagResourceResponse).when(this.proxy).injectCredentialsAndInvokeV2(any(TagResourceRequest.class), any());

        final ListResourcesInProtectionGroupResponse listResourcesInProtectionGroupResponse = ListResourcesInProtectionGroupResponse.builder()
                .resourceArns(ProtectionGroupTestData.MEMBERS).build();

        doReturn(listResourcesInProtectionGroupResponse).when(this.proxy).injectCredentialsAndInvokeV2(any(ListResourcesInProtectionGroupRequest.class), any());

        final ListTagsForResourceResponse listTagsForResourceResponse = ListTagsForResourceResponse.builder()
                .tags(
                        software.amazon.awssdk.services.shield.model.Tag.builder()
                                .key("k1").value("v1").build(),
                        software.amazon.awssdk.services.shield.model.Tag.builder()
                                .key("k2").value("v2").build()
                ).build();

        doReturn(listTagsForResourceResponse).when(this.proxy).injectCredentialsAndInvokeV2(any(ListTagsForResourceRequest.class), any());

        final DescribeProtectionGroupResponse describeProtectionGroupResponse =
                DescribeProtectionGroupResponse.builder()
                        .protectionGroup(
                                ProtectionGroup.builder()
                                        .protectionGroupId(ProtectionGroupTestData.PROTECTION_GROUP_ID)
                                        .protectionGroupArn(ProtectionGroupTestData.PROTECTION_GROUP_ARN)
                                        .resourceType(ProtectionGroupTestData.RESOURCE_TYPE)
                                        .aggregation(ProtectionGroupTestData.AGGREGATION)
                                        .pattern(ProtectionGroupTestData.PATTERN)
                                        .members(ProtectionGroupTestData.MEMBERS)
                                        .build())
                        .build();

        doReturn(describeProtectionGroupResponse)
                .when(this.proxy).injectCredentialsAndInvokeV2(any(DescribeProtectionGroupRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = this.updateHandler.handleRequest(this.proxy, request, null, this.logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(ProtectionGroupTestData.RESOURCE_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_with_untags_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .previousResourceState(this.resourceModel)
                        .desiredResourceState(ResourceModel.builder()
                                .protectionGroupId(ProtectionGroupTestData.PROTECTION_GROUP_ID)
                                .protectionGroupArn(ProtectionGroupTestData.PROTECTION_GROUP_ARN)
                                .pattern(ProtectionGroupTestData.PATTERN)
                                .aggregation(ProtectionGroupTestData.AGGREGATION)
                                .resourceType(ProtectionGroupTestData.RESOURCE_TYPE)
                                .members(ProtectionGroupTestData.MEMBERS)
                                .build())
                        .nextToken(ProtectionGroupTestData.NEXT_TOKEN)
                        .build();

        final UpdateProtectionGroupResponse updateProtectionGroupResponse =
                UpdateProtectionGroupResponse.builder()
                        .build();

        doReturn(updateProtectionGroupResponse)
                .when(this.proxy).injectCredentialsAndInvokeV2(any(UpdateProtectionGroupRequest.class), any());

        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder().build();

        doReturn(untagResourceResponse).when(this.proxy).injectCredentialsAndInvokeV2(any(UntagResourceRequest.class), any());

        final ListResourcesInProtectionGroupResponse listResourcesInProtectionGroupResponse = ListResourcesInProtectionGroupResponse.builder()
                .resourceArns(ProtectionGroupTestData.MEMBERS).build();

        doReturn(listResourcesInProtectionGroupResponse).when(this.proxy).injectCredentialsAndInvokeV2(any(ListResourcesInProtectionGroupRequest.class), any());

        final ListTagsForResourceResponse listTagsForResourceResponse = ListTagsForResourceResponse.builder().build();

        doReturn(listTagsForResourceResponse).when(this.proxy).injectCredentialsAndInvokeV2(any(ListTagsForResourceRequest.class), any());

        final DescribeProtectionGroupResponse describeProtectionGroupResponse =
                DescribeProtectionGroupResponse.builder()
                        .protectionGroup(
                                ProtectionGroup.builder()
                                        .protectionGroupId(ProtectionGroupTestData.PROTECTION_GROUP_ID)
                                        .protectionGroupArn(ProtectionGroupTestData.PROTECTION_GROUP_ARN)
                                        .resourceType(ProtectionGroupTestData.RESOURCE_TYPE)
                                        .aggregation(ProtectionGroupTestData.AGGREGATION)
                                        .pattern(ProtectionGroupTestData.PATTERN)
                                        .members(ProtectionGroupTestData.MEMBERS)
                                        .build())
                        .build();

        doReturn(describeProtectionGroupResponse)
                .when(this.proxy).injectCredentialsAndInvokeV2(any(DescribeProtectionGroupRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = this.updateHandler.handleRequest(this.proxy, request, null, this.logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getTags()).isEmpty();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
