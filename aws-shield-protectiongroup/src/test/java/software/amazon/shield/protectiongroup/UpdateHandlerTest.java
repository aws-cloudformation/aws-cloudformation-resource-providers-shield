package software.amazon.shield.protectiongroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupResponse;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DisableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.shield.model.ProtectionGroup;
import software.amazon.awssdk.services.shield.model.UpdateApplicationLayerAutomaticResponseRequest;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

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
        this.logger = mock(Logger.class, withSettings().verboseLogging());

        this.updateHandler = new UpdateHandler(mock(ShieldClient.class));
        this.resourceModel = ProtectionGroupTestData.RESOURCE_MODEL;
    }

    @Test
    public void updateNoFields() {
        final DescribeProtectionGroupResponse describeProtectionGroupResponse = DescribeProtectionGroupResponse.builder()
                .protectionGroup(ProtectionGroup.builder()
                    .resourceType(this.resourceModel.getResourceType())
                    .protectionGroupId(this.resourceModel.getProtectionGroupId())
                    .protectionGroupArn(this.resourceModel.getProtectionGroupArn())
                    .aggregation(this.resourceModel.getAggregation())
                    .members(this.resourceModel.getMembers())
                    .pattern(this.resourceModel.getPattern())
                    .build()
                ).build();
        when(this.proxy
            .injectCredentialsAndInvokeV2(any(), any()))
            .thenAnswer(i -> {
                if (i.getArgument(0) instanceof DescribeProtectionGroupRequest) {
                    return describeProtectionGroupResponse;
                } else if (i.getArgument(0) instanceof ListTagsForResourceRequest) {
                    return ListTagsForResourceResponse.builder().build();
                }
                throw new AssertionError("unknown invocation: " + i.getArgument(0));
            });

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(this.resourceModel)
                .desiredResourceState(this.resourceModel)
                .nextToken(ProtectionGroupTestData.NEXT_TOKEN)
                .build();
        final UpdateProtectionGroupResponse updateProtectionGroupResponse =
            UpdateProtectionGroupResponse.builder()
                .build();

        doReturn(updateProtectionGroupResponse)
            .when(this.proxy).injectCredentialsAndInvokeV2(any(UpdateProtectionGroupRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
            = this.updateHandler.handleRequest(this.proxy, request, null, this.logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
