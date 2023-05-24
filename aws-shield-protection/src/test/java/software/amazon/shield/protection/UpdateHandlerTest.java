package software.amazon.shield.protection;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateHealthCheckRequest;
import software.amazon.awssdk.services.shield.model.AssociateHealthCheckResponse;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseResponse;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.awssdk.services.shield.model.TagResourceRequest;
import software.amazon.awssdk.services.shield.model.TagResourceResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
        proxy = spy(new AmazonWebServicesClientProxy(
            new LoggerProxy(),
            new Credentials("accessKey", "secretKey", "token"),
            () -> Duration.ofSeconds(600).toMillis()
        ));
        this.logger = mock(Logger.class, withSettings().verboseLogging());

        this.updateHandler = new UpdateHandler(mock(ShieldClient.class, withSettings().verboseLogging()));
        ShieldAPIChainableRemoteCall.JITTER_SECONDS = 0;
    }

    @Test
    public void updateAllFields() {
        doReturn(EnableApplicationLayerAutomaticResponseResponse.builder().build()).when(this.proxy)
            .injectCredentialsAndInvokeV2(any(EnableApplicationLayerAutomaticResponseRequest.class), any());
        doReturn(AssociateHealthCheckResponse.builder().build()).when(this.proxy)
            .injectCredentialsAndInvokeV2(any(AssociateHealthCheckRequest.class), any());
        doReturn(TagResourceResponse.builder().build()).when(this.proxy).injectCredentialsAndInvokeV2(eq(
            TagResourceRequest.builder()
                .resourceARN("arn:aws:shield::123456789012:protection/TEST_PROTECTION_ID")
                .tags(
                    Tag.builder()
                        .key("k1")
                        .value("v1")
                        .build(),
                    Tag.builder()
                        .key("k2")
                        .value("v2")
                        .build()
                )
                .build()
        ), any());

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(ProtectionTestData.ACCOUNT_ID)
                .previousResourceState(ProtectionTestData.RESOURCE_MODEL_1.toBuilder()
                    .healthCheckArns(null)
                    .applicationLayerAutomaticResponseConfiguration(null)
                    .tags(null)
                    .build())
                .desiredResourceState(ProtectionTestData.RESOURCE_MODEL_1)
                .nextToken(ProtectionTestData.NEXT_TOKEN)
                .build();
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
                    .protectionId(ProtectionTestData.PROTECTION_ID)
                    .protectionArn(ProtectionTestData.PROTECTION_ARN)
                    .resourceArn(ProtectionTestData.RESOURCE_ARN_1)
                    .build())
                .desiredResourceState(ResourceModel.builder()
                    .protectionId(ProtectionTestData.PROTECTION_ID)
                    .protectionArn(ProtectionTestData.PROTECTION_ARN)
                    .resourceArn(ProtectionTestData.RESOURCE_ARN_1)
                    .build())
                .nextToken(ProtectionTestData.NEXT_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = this.updateHandler.handleRequest(this.proxy, request, null, this.logger);

        final ResourceModel expectedReturningModel = ResourceModel.builder()
            .protectionId(ProtectionTestData.PROTECTION_ID)
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
