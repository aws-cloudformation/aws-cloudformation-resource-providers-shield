package software.amazon.shield.protection;

import java.time.Duration;

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
import software.amazon.awssdk.services.shield.model.CreateProtectionRequest;
import software.amazon.awssdk.services.shield.model.CreateProtectionResponse;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionResponse;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseResponse;
import software.amazon.awssdk.services.shield.model.Protection;
import software.amazon.awssdk.services.shield.model.ResponseAction;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;
import software.amazon.shield.protection.helper.ProtectionTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

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
        proxy = spy(new AmazonWebServicesClientProxy(
            new LoggerProxy(),
            new Credentials("accessKey", "secretKey", "token"),
            () -> Duration.ofSeconds(600).toMillis()
        ));
        this.logger = mock(Logger.class);

        this.createHandler = new CreateHandler(mock(ShieldClient.class));
        this.resourceModel = ProtectionTestData.RESOURCE_MODEL_1;
        ShieldAPIChainableRemoteCall.JITTER_SECONDS = 0;
    }

    @Test
    public void handleRequest_Success() {
        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(this.resourceModel)
                .nextToken(ProtectionTestData.NEXT_TOKEN)
                .build();

        final CreateProtectionResponse createProtectionResponse =
            CreateProtectionResponse.builder()
                .protectionId(ProtectionTestData.PROTECTION_ID)
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
                            ProtectionTestData.HEALTH_CHECK_ID_2
                        )
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

        doReturn(createProtectionResponse)
            .when(this.proxy)
            .injectCredentialsAndInvokeV2(any(CreateProtectionRequest.class), any());
        doReturn(describeProtectionResponse)
            .when(this.proxy)
            .injectCredentialsAndInvokeV2(any(DescribeProtectionRequest.class), any());

        doReturn(AssociateHealthCheckResponse.builder().build())
            .when(this.proxy)
            .injectCredentialsAndInvokeV2(any(AssociateHealthCheckRequest.class), any());
        doReturn(EnableApplicationLayerAutomaticResponseResponse.builder().build())
            .when(this.proxy)
            .injectCredentialsAndInvokeV2(any(EnableApplicationLayerAutomaticResponseRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response =
            this.createHandler.handleRequest(this.proxy, request, null, this.logger);

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
    public void handleRequest_noApplicationLayerAutomaticResponse() {
        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder()
                    .name(this.resourceModel.getName())
                    .protectionId(this.resourceModel.getProtectionId())
                    .protectionArn(this.resourceModel.getProtectionArn())
                    .resourceArn(this.resourceModel.getResourceArn())
                    .tags(this.resourceModel.getTags())
                    .healthCheckArns(this.resourceModel.getHealthCheckArns())
                    .build()
                )
                .build();

        final CreateProtectionResponse createProtectionResponse =
            CreateProtectionResponse.builder()
                .protectionId(ProtectionTestData.PROTECTION_ID)
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
                            ProtectionTestData.HEALTH_CHECK_ID_2
                        )
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

        doReturn(createProtectionResponse)
            .when(this.proxy)
            .injectCredentialsAndInvokeV2(any(CreateProtectionRequest.class), any());
        doReturn(describeProtectionResponse)
            .when(this.proxy)
            .injectCredentialsAndInvokeV2(any(DescribeProtectionRequest.class), any());

        doReturn(AssociateHealthCheckResponse.builder().build())
            .when(this.proxy)
            .injectCredentialsAndInvokeV2(any(AssociateHealthCheckRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response =
            this.createHandler.handleRequest(this.proxy, request, null, this.logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
