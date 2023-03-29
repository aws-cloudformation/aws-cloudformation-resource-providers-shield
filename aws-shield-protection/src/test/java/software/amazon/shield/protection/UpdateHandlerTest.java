package software.amazon.shield.protection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ApplicationLayerAutomaticResponseConfiguration;
import software.amazon.awssdk.services.shield.model.AssociateHealthCheckRequest;
import software.amazon.awssdk.services.shield.model.AssociateHealthCheckResponse;
import software.amazon.awssdk.services.shield.model.BlockAction;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionResponse;
import software.amazon.awssdk.services.shield.model.DisableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseResponse;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.shield.model.Protection;
import software.amazon.awssdk.services.shield.model.ResponseAction;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.awssdk.services.shield.model.TagResourceRequest;
import software.amazon.awssdk.services.shield.model.TagResourceResponse;
import software.amazon.awssdk.services.shield.model.UpdateApplicationLayerAutomaticResponseRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.protection.helper.ProtectionTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    public void setup() {
        this.proxy = mock(AmazonWebServicesClientProxy.class, withSettings().verboseLogging());
        this.logger = mock(Logger.class, withSettings().verboseLogging());

        this.updateHandler = new UpdateHandler(mock(ShieldClient.class, withSettings().verboseLogging()));
    }

    @Test
    public void updateAllFields() {
        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ProtectionTestData.RESOURCE_MODEL_1.toBuilder()
                    .healthCheckArns(null)
                    .applicationLayerAutomaticResponseConfiguration(null)
                    .tags(null)
                    .build())
                .desiredResourceState(ProtectionTestData.RESOURCE_MODEL_1)
                .nextToken(ProtectionTestData.NEXT_TOKEN)
                .build();

        when(this.proxy
            .injectCredentialsAndInvokeV2(any(), any()))
            .thenAnswer(i -> {
                if (i.getArgument(0) instanceof DescribeProtectionRequest) {
                    return DescribeProtectionResponse.builder()
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
                } else if (i.getArgument(0) instanceof ListTagsForResourceRequest) {
                    return ListTagsForResourceResponse.builder()
                        .tags(
                            Tag.builder().key("k1").value("v1").build(),
                            Tag.builder().key("k2").value("v2").build())
                        .build();
                } else if (i.getArgument(0) instanceof EnableApplicationLayerAutomaticResponseRequest) {
                    return EnableApplicationLayerAutomaticResponseResponse.builder().build();
                } else if (i.getArgument(0) instanceof AssociateHealthCheckRequest) {
                    return AssociateHealthCheckResponse.builder().build();
                } else if (i.getArgument(0) instanceof TagResourceRequest) {
                    assertThat(((TagResourceRequest) i.getArgument(0)).tags().toString()).isEqualTo(
                        "[Tag(Key=k1, Value=v1), Tag(Key=k2, Value=v2)]");
                    return TagResourceResponse.builder().build();
                }
                throw new AssertionError("unknown invocation: " + i.getArgument(0));
            });

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

    @Test
    public void minimalUpdates() {
        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder()
                    .protectionArn(ProtectionTestData.PROTECTION_ARN)
                    .resourceArn(ProtectionTestData.RESOURCE_ARN_1)
                    .build())
                .desiredResourceState(ResourceModel.builder()
                    .protectionArn(ProtectionTestData.PROTECTION_ARN)
                    .resourceArn(ProtectionTestData.RESOURCE_ARN_1)
                    .build())
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
                        .build())
                .build();

        when(this.proxy
            .injectCredentialsAndInvokeV2(any(), any()))
            .thenAnswer(i -> {
                if (i.getArgument(0) instanceof DescribeProtectionRequest) {
                    return describeProtectionResponse;
                } else if (i.getArgument(0) instanceof ListTagsForResourceRequest) {
                    return ListTagsForResourceResponse.builder().build();
                } else if (
                    i.getArgument(0) instanceof EnableApplicationLayerAutomaticResponseRequest
                        || i.getArgument(0) instanceof DisableApplicationLayerAutomaticResponseRequest
                        || i.getArgument(0) instanceof UpdateApplicationLayerAutomaticResponseRequest
                ) {
                    throw new AssertionError("l4 protection must not invoke l7 apis");
                }
                throw new AssertionError("unknown invocation: " + i.getArgument(0));
            });

        final ProgressEvent<ResourceModel, CallbackContext> response
            = this.updateHandler.handleRequest(this.proxy, request, null, this.logger);

        final ResourceModel expectedReturningModel = ResourceModel.builder()
            .protectionId(ProtectionTestData.PROTECTION_ID)
            .name(ProtectionTestData.NAME_1)
            .protectionArn(ProtectionTestData.PROTECTION_ARN)
            .resourceArn(ProtectionTestData.RESOURCE_ARN_1)
            .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedReturningModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
