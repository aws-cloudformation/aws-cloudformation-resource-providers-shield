package software.amazon.shield.protection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ApplicationLayerAutomaticResponseConfiguration;
import software.amazon.awssdk.services.shield.model.BlockAction;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionResponse;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseResponse;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.shield.model.Protection;
import software.amazon.awssdk.services.shield.model.ResponseAction;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.protection.helper.ProtectionTestData;

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
        this.resourceModel = ProtectionTestData.RESOURCE_MODEL_1;
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(this.resourceModel)
                        .nextToken(ProtectionTestData.NEXT_TOKEN)
                        .build();

        final DescribeProtectionResponse describeProtectionResponse =
                DescribeProtectionResponse.builder()
                        .protection(
                                Protection.builder()
                                        .name(ProtectionTestData.NAME_1)
                                        .resourceArn(ProtectionTestData.RESOURCE_ARN_1)
                                        .protectionArn(ProtectionTestData.PROTECTION_ARN)
                                        .id(ProtectionTestData.PROTECTION_ID)
                                        .healthCheckIds(
                                                ProtectionTestData.HEALTH_CHECK_ID_1,
                                                ProtectionTestData.HEALTH_CHECK_ID_2)
                                        .applicationLayerAutomaticResponseConfiguration(
                                                ApplicationLayerAutomaticResponseConfiguration.builder()
                                                        .action(
                                                                ResponseAction.builder()
                                                                        .block(BlockAction.builder().build())
                                                                        .build())
                                                        .status(ProtectionTestData.ENABLED)
                                                        .build())
                                        .build())
                        .build();

        doReturn(EnableApplicationLayerAutomaticResponseResponse.builder().build())
                .when(this.proxy)
                .injectCredentialsAndInvokeV2(any(EnableApplicationLayerAutomaticResponseRequest.class), any());

        registerListTags();

        doReturn(describeProtectionResponse)
                .when(this.proxy)
                .injectCredentialsAndInvokeV2(any(DescribeProtectionRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = this.updateHandler.handleRequest(this.proxy, request, null, this.logger);

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
