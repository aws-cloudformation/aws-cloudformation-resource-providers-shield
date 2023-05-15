package software.amazon.shield.protectiongroup;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.TagResourceRequest;
import software.amazon.awssdk.services.shield.model.TagResourceResponse;
import software.amazon.awssdk.services.shield.model.UntagResourceRequest;
import software.amazon.awssdk.services.shield.model.UntagResourceResponse;
import software.amazon.awssdk.services.shield.model.UpdateProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.UpdateProtectionGroupResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.protectiongroup.helper.ProtectionGroupTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

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
        proxy = spy(new AmazonWebServicesClientProxy(new LoggerProxy(),
            new Credentials("accessKey", "secretKey", "token"),
            () -> Duration.ofSeconds(600).toMillis()));
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

        doReturn(tagResourceResponse).when(this.proxy)
            .injectCredentialsAndInvokeV2(any(TagResourceRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
            = this.updateHandler.handleRequest(this.proxy, request, new CallbackContext(), this.logger);

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

        doReturn(untagResourceResponse).when(this.proxy)
            .injectCredentialsAndInvokeV2(any(UntagResourceRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
            = this.updateHandler.handleRequest(this.proxy, request, new CallbackContext(), this.logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
