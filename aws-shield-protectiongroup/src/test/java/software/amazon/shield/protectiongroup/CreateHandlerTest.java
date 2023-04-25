package software.amazon.shield.protectiongroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.CreateProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.CreateProtectionGroupResponse;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupResponse;
import software.amazon.awssdk.services.shield.model.ListResourcesInProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.ListResourcesInProtectionGroupResponse;
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
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler createHandler;
    private ResourceModel resourceModel;

    @BeforeEach
    public void setup() {
        this.proxy = mock(AmazonWebServicesClientProxy.class);
        this.logger = mock(Logger.class);

        this.createHandler = new CreateHandler(mock(ShieldClient.class));
        this.resourceModel = ProtectionGroupTestData.RESOURCE_MODEL;
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(this.resourceModel)
                        .awsAccountId("111222")
                        .nextToken(ProtectionGroupTestData.NEXT_TOKEN)
                        .build();

        final CreateProtectionGroupResponse createProtectionGroupResponse =
                CreateProtectionGroupResponse.builder()
                        .build();

        doReturn(createProtectionGroupResponse)
                .when(this.proxy).injectCredentialsAndInvokeV2(any(CreateProtectionGroupRequest.class), any());

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

        final ListResourcesInProtectionGroupResponse listResourcesInProtectionGroupResponse =
                ListResourcesInProtectionGroupResponse.builder()
                                .resourceArns(ProtectionGroupTestData.MEMBERS)
                                        .build();
        doReturn(listResourcesInProtectionGroupResponse)
                .when(this.proxy).injectCredentialsAndInvokeV2(any(ListResourcesInProtectionGroupRequest.class), any());

        registerListTags();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                this.createHandler.handleRequest(this.proxy, request, null, this.logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(this.resourceModel);
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
