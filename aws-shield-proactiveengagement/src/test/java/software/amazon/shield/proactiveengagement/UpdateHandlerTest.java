package software.amazon.shield.proactiveengagement;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.DisableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.DisableProactiveEngagementResponse;
import software.amazon.awssdk.services.shield.model.EnableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.EnableProactiveEngagementResponse;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.awssdk.services.shield.model.UpdateEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.UpdateEmergencyContactSettingsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;
import software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Mock
    private ShieldClient shieldClient;

    private UpdateHandler updateHandler;

    private ProxyClient<ShieldClient> proxyClient;

    private CallbackContext callbackContext;

    private ResourceModel model;

    @BeforeEach
    public void setup() {
        proxy = spy(new AmazonWebServicesClientProxy(new LoggerProxy(),
            MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis()));
        logger = mock(Logger.class);
        updateHandler = new UpdateHandler(shieldClient);
        proxyClient = ProactiveEngagementTestHelper.MOCK_PROXY(proxy, shieldClient);
        callbackContext = new CallbackContext();
        model = ResourceModel.builder().accountId(ProactiveEngagementTestHelper.accountId).build();
        ShieldAPIChainableRemoteCall.JITTER_SECONDS = 0;
    }

    @Test
    public void handleRequest_EnableProactiveEngagement() {
        // Mock describe subscription
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
            .subscription(Subscription.builder()
                .proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED)
                .build())
            .build();
        doReturn(describeSubscriptionResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());

        // Mock describe emergency contact list
        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
            DescribeEmergencyContactSettingsResponse.builder()
                .emergencyContactList(ProactiveEngagementTestHelper.emergencyContactList)
                .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        // Mock update emergency contact list
        final UpdateEmergencyContactSettingsResponse updateEmergencyContactSettingsResponse =
            UpdateEmergencyContactSettingsResponse.builder()
                .build();
        doReturn(updateEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(UpdateEmergencyContactSettingsRequest.class), any());

        // Mock enable proactive engagement
        final EnableProactiveEngagementResponse enableProactiveEngagementResponse =
            EnableProactiveEngagementResponse.builder()
                .build();
        doReturn(enableProactiveEngagementResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(EnableProactiveEngagementRequest.class), any());

        // Mock change propagation
        final ProgressEvent<ResourceModel, CallbackContext> inProgressEvent =
            ProgressEvent.progress(model, callbackContext);

        model = ResourceModel.builder()
            .accountId(ProactiveEngagementTestHelper.accountId)
            .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED.toString())
            .emergencyContactList(ProactiveEngagementTestHelper.convertEmergencyContactList(
                ProactiveEngagementTestHelper.newEmergencyContactList))
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(ProactiveEngagementTestHelper.accountId)
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = updateHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(ProgressEvent.defaultSuccessHandler(model));
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
            .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.ENABLED.toString());
        assertThat(response.getResourceModel()
            .getEmergencyContactList()).isEqualTo(ProactiveEngagementTestHelper.convertEmergencyContactList(
            ProactiveEngagementTestHelper.newEmergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DisableProactiveEngagement() {
        // Mock describe subscription
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
            .subscription(Subscription.builder()
                .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED)
                .build())
            .build();
        doReturn(describeSubscriptionResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());

        // Mock describe emergency contact list
        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
            DescribeEmergencyContactSettingsResponse.builder()
                .emergencyContactList(ProactiveEngagementTestHelper.emergencyContactList)
                .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        // Mock update emergency contact list
        final UpdateEmergencyContactSettingsResponse updateEmergencyContactSettingsResponse =
            UpdateEmergencyContactSettingsResponse.builder()
                .build();
        doReturn(updateEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(UpdateEmergencyContactSettingsRequest.class), any());

        // Mock disable proactive engagement
        final DisableProactiveEngagementResponse disableProactiveEngagementResponse =
            DisableProactiveEngagementResponse.builder()
                .build();
        doReturn(disableProactiveEngagementResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DisableProactiveEngagementRequest.class), any());

        // Mock change propagation
        final ProgressEvent<ResourceModel, CallbackContext> inProgressEvent =
            ProgressEvent.progress(model, callbackContext);

        model = ResourceModel.builder()
            .accountId(ProactiveEngagementTestHelper.accountId)
            .proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED.toString())
            .emergencyContactList(ProactiveEngagementTestHelper.convertEmergencyContactList(
                ProactiveEngagementTestHelper.newEmergencyContactList))
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(ProactiveEngagementTestHelper.accountId)
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = updateHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
            .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.DISABLED.toString());
        assertThat(response.getResourceModel()
            .getEmergencyContactList()).isEqualTo(ProactiveEngagementTestHelper.convertEmergencyContactList(
            ProactiveEngagementTestHelper.newEmergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateContactListsUnderEngagementEnabled() {
        // Mock describe subscription
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
            .subscription(Subscription.builder()
                .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED)
                .build())
            .build();
        doReturn(describeSubscriptionResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());

        // Mock describe emergency contact list
        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
            DescribeEmergencyContactSettingsResponse.builder()
                .emergencyContactList(ProactiveEngagementTestHelper.emergencyContactList)
                .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        // Mock update emergency contact list
        final UpdateEmergencyContactSettingsResponse updateEmergencyContactSettingsResponse =
            UpdateEmergencyContactSettingsResponse.builder()
                .build();
        doReturn(updateEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(UpdateEmergencyContactSettingsRequest.class), any());

        final EnableProactiveEngagementResponse enableProactiveEngagementResponse =
            EnableProactiveEngagementResponse.builder()
                .build();
        doReturn(enableProactiveEngagementResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(EnableProactiveEngagementRequest.class), any());

        // Mock change propagation
        final ProgressEvent<ResourceModel, CallbackContext> inProgressEvent =
            ProgressEvent.progress(model, callbackContext);

        model = ResourceModel.builder()
            .accountId(ProactiveEngagementTestHelper.accountId)
            .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED.toString())
            .emergencyContactList(ProactiveEngagementTestHelper.convertEmergencyContactList(
                ProactiveEngagementTestHelper.newEmergencyContactList))
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(ProactiveEngagementTestHelper.accountId)
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = updateHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
            .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.ENABLED.toString());
        assertThat(response.getResourceModel()
            .getEmergencyContactList()).isEqualTo(ProactiveEngagementTestHelper.convertEmergencyContactList(
            ProactiveEngagementTestHelper.newEmergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateContactListsUnderEngagementDisabled() {
        // Mock describe subscription
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
            .subscription(Subscription.builder()
                .proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED)
                .build())
            .build();
        doReturn(describeSubscriptionResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());

        // Mock describe emergency contact list
        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
            DescribeEmergencyContactSettingsResponse.builder()
                .emergencyContactList(ProactiveEngagementTestHelper.emergencyContactList)
                .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        // Mock update emergency contact list
        final UpdateEmergencyContactSettingsResponse updateEmergencyContactSettingsResponse =
            UpdateEmergencyContactSettingsResponse.builder()
                .build();
        doReturn(updateEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(UpdateEmergencyContactSettingsRequest.class), any());

        final DisableProactiveEngagementResponse disableProactiveEngagementResponse =
            DisableProactiveEngagementResponse.builder()
                .build();
        doReturn(disableProactiveEngagementResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DisableProactiveEngagementRequest.class), any());

        // Mock change propagation
        final ProgressEvent<ResourceModel, CallbackContext> inProgressEvent =
            ProgressEvent.progress(model, callbackContext);

        model = ResourceModel.builder()
            .accountId(ProactiveEngagementTestHelper.accountId)
            .proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED.toString())
            .emergencyContactList(ProactiveEngagementTestHelper.convertEmergencyContactList(
                ProactiveEngagementTestHelper.newEmergencyContactList))
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(ProactiveEngagementTestHelper.accountId)
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = updateHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
            .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.DISABLED.toString());
        assertThat(response.getResourceModel()
            .getEmergencyContactList()).isEqualTo(ProactiveEngagementTestHelper.convertEmergencyContactList(
            ProactiveEngagementTestHelper.newEmergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateContactListsAndEngagementStatus() {
        // Mock describe subscription
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
            .subscription(Subscription.builder()
                .proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED)
                .build())
            .build();
        doReturn(describeSubscriptionResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());

        // Mock describe emergency contact list
        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
            DescribeEmergencyContactSettingsResponse.builder()
                .emergencyContactList(ProactiveEngagementTestHelper.emergencyContactList)
                .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        // Mock update emergency contact list
        final UpdateEmergencyContactSettingsResponse updateEmergencyContactSettingsResponse =
            UpdateEmergencyContactSettingsResponse.builder()
                .build();
        doReturn(updateEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(UpdateEmergencyContactSettingsRequest.class), any());

        // Mock enable proactive engagement
        final EnableProactiveEngagementResponse enableProactiveEngagementResponse =
            EnableProactiveEngagementResponse.builder()
                .build();
        doReturn(enableProactiveEngagementResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(EnableProactiveEngagementRequest.class), any());

        // Mock change propagation
        final ProgressEvent<ResourceModel, CallbackContext> inProgressEvent =
            ProgressEvent.progress(model, callbackContext);

        model = ResourceModel.builder()
            .accountId(ProactiveEngagementTestHelper.accountId)
            .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED.toString())
            .emergencyContactList(ProactiveEngagementTestHelper.convertEmergencyContactList(
                ProactiveEngagementTestHelper.newEmergencyContactList))
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(ProactiveEngagementTestHelper.accountId)
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = updateHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
            .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.ENABLED.toString());
        assertThat(response.getResourceModel()
            .getEmergencyContactList()).isEqualTo(ProactiveEngagementTestHelper.convertEmergencyContactList(
            ProactiveEngagementTestHelper.newEmergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AccountNotFoundFailure() {
        final ResourceModel model = ResourceModel.builder()
            .accountId(ProactiveEngagementTestHelper.accountId)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            updateHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
