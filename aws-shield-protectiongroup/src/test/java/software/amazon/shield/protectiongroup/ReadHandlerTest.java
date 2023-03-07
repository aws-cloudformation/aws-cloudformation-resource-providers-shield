package software.amazon.shield.protectiongroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupResponse;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.shield.model.ProtectionGroup;
import software.amazon.awssdk.services.shield.model.Tag;
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
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ReadHandler readHandler;
    private ResourceModel resourceModel;

    @BeforeEach
    public void setup() {
        this.proxy = mock(AmazonWebServicesClientProxy.class);
        this.logger = mock(Logger.class);

        this.readHandler = new ReadHandler(mock(ShieldClient.class));
        this.resourceModel = ProtectionGroupTestData.RESOURCE_MODEL;
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(this.resourceModel)
                        .nextToken(ProtectionGroupTestData.NEXT_TOKEN)
                        .build();

        final DescribeProtectionGroupResponse createProtectionGroupResponse =
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

        doReturn(createProtectionGroupResponse)
                .when(this.proxy).injectCredentialsAndInvokeV2(any(DescribeProtectionGroupRequest.class), any());

        registerListTags();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                this.readHandler.handleRequest(this.proxy, request, null, this.logger);

       assertThat(response).isNotNull();
       assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
       assertThat(response.getCallbackContext()).isNull();
       assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
       assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
       assertThat(response.getResourceModels()).isNull();
       assertThat(response.getMessage()).isNull();
       assertThat(response.getErrorCode()).isNull();
    }

    private void registerListTags() {

        ListTagsForResourceResponse tagResponse =
                ListTagsForResourceResponse.builder()
                        .tags(
                                Tag.builder().key("k1").value("v1").build(),
                                Tag.builder().key("k2").value("v2").build())
                        .build();

        doReturn(tagResponse)
                .when(this.proxy)
                .injectCredentialsAndInvokeV2(any(ListTagsForResourceRequest.class), any());
    }
}
