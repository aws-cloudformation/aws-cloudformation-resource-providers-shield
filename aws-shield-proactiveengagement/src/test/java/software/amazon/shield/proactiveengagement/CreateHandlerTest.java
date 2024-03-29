package software.amazon.shield.proactiveengagement;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateProactiveEngagementDetailsRequest;
import software.amazon.awssdk.services.shield.model.AssociateProactiveEngagementDetailsResponse;
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
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;
import software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import static software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Mock
    private ShieldClient shieldClient;

    private CreateHandler createHandler;

    private ProxyClient<ShieldClient> proxyClient;

    private CallbackContext callbackContext;

    private ResourceModel model;

    @BeforeEach
    public void setup() {
        proxy = spy(new AmazonWebServicesClientProxy(new LoggerProxy(),
            MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis()));
        logger = mock(Logger.class);
        createHandler = new CreateHandler(shieldClient);
        proxyClient = ProactiveEngagementTestHelper.MOCK_PROXY(proxy, shieldClient);
        callbackContext = new CallbackContext();
        model = ResourceModel.builder().accountId(ProactiveEngagementTestHelper.accountId).build();
        ShieldAPIChainableRemoteCall.JITTER_SECONDS = 0;
    }

    @Test
    public void handleRequest_SimpleSuccess_With_ProactiveEngagementStatus_Null() {
        // Mock describe subscription
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
                .subscription(Subscription.builder().build())
                .build();
        final DescribeSubscriptionResponse describeSubscriptionResponse2 = DescribeSubscriptionResponse.builder()
                .subscription(Subscription.builder()
                        .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED)
                        .build())
                .build();
        doReturn(describeSubscriptionResponse).doReturn(describeSubscriptionResponse2)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());

        // Mock describe emergency contact list
        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
                DescribeEmergencyContactSettingsResponse.builder()
                        .emergencyContactList(ProactiveEngagementTestHelper.emergencyContactList)
                        .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        // Mock associate proactive engagement with account
        final AssociateProactiveEngagementDetailsResponse associateProactiveEngagementDetailsResponse =
                AssociateProactiveEngagementDetailsResponse.builder()
                        .build();

        doReturn(associateProactiveEngagementDetailsResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(AssociateProactiveEngagementDetailsRequest.class), any());

        // Mock change propagation
        final ProgressEvent<ResourceModel, CallbackContext> inProgressEvent =
                ProgressEvent.defaultInProgressHandler(callbackContext, 0, model);

        handleCreateWithEnabled();

        handleCreateWithDisabled();
    }

    @Test
    public void handleRequest_SimpleSuccess_Already_Configured() {
        // Mock describe subscription
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
                .subscription(Subscription.builder().proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED).build()).build();
        doReturn(describeSubscriptionResponse)
                .when(proxy).injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());


        // Mock describe emergency contact list
        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
                DescribeEmergencyContactSettingsResponse.builder()
                        .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        final UpdateEmergencyContactSettingsResponse updateEmergencyContactSettingsResponse =
                UpdateEmergencyContactSettingsResponse.builder().build();

        doReturn(updateEmergencyContactSettingsResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateEmergencyContactSettingsRequest.class), any());

        final EnableProactiveEngagementResponse enableProactiveEngagementResponse =
                EnableProactiveEngagementResponse.builder().build();

        doReturn(enableProactiveEngagementResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(EnableProactiveEngagementRequest.class), any());

        final DisableProactiveEngagementResponse disableProactiveEngagementResponse =
                DisableProactiveEngagementResponse.builder().build();

        doReturn(disableProactiveEngagementResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DisableProactiveEngagementRequest.class), any());
        // Mock change propagation
        final ProgressEvent<ResourceModel, CallbackContext> inProgressEvent =
                ProgressEvent.defaultInProgressHandler(callbackContext, 0, model);

        handleCreateWithEnabled();

        handleCreateWithDisabled();
    }

    @Test
    public void handleRequest_AccountConflict() {
        // Mock describe subscription
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
            .subscription(Subscription.builder()
                .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED)
                .build())
            .build();
        doReturn(describeSubscriptionResponse).doReturn(describeSubscriptionResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());
        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
            DescribeEmergencyContactSettingsResponse.builder()
                .emergencyContactList(ProactiveEngagementTestHelper.emergencyContactList)
                .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        model = ResourceModel.builder()
            .accountId(ProactiveEngagementTestHelper.accountId)
            .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED.toString())
            .emergencyContactList(HandlerHelper.convertSDKEmergencyContactList(ProactiveEngagementTestHelper.emergencyContactList))
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(ProactiveEngagementTestHelper.accountId)
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = createHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ResourceConflict);
        assertThat(response.getMessage()).containsIgnoringCase(
            "Proactive engagement is already configured on the account.");
    }

    public void handleCreateWithEnabled() {
        model = ResourceModel.builder()
                .accountId(ProactiveEngagementTestHelper.accountId)
                .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED.toString())
                .emergencyContactList(HandlerHelper.convertSDKEmergencyContactList(ProactiveEngagementTestHelper.emergencyContactList))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(ProactiveEngagementTestHelper.accountId)
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = createHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
                .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.ENABLED.toString());
        assertThat(response.getResourceModel()
                .getEmergencyContactList()).isEqualTo(HandlerHelper.convertSDKEmergencyContactList(
                ProactiveEngagementTestHelper.emergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    public void handleCreateWithDisabled() {
        model = ResourceModel.builder()
                .accountId(ProactiveEngagementTestHelper.accountId)
                .proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED.toString())
                .emergencyContactList(HandlerHelper.convertSDKEmergencyContactList(ProactiveEngagementTestHelper.emergencyContactList))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(ProactiveEngagementTestHelper.accountId)
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = createHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
                .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.DISABLED.toString());
        assertThat(response.getResourceModel()
                .getEmergencyContactList()).isEqualTo(HandlerHelper.convertSDKEmergencyContactList(
                ProactiveEngagementTestHelper.emergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
