package software.amazon.shield.proactiveengagement;

import java.time.Duration;

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
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;
import software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

    private CallChain.RequestMaker<ShieldClient, ResourceModel, CallbackContext> init;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        createHandler = new CreateHandler(shieldClient);
        proxyClient = ProactiveEngagementTestHelper.MOCK_PROXY(proxy, shieldClient);
        callbackContext = new CallbackContext();
        model = ResourceModel.builder().accountId(ProactiveEngagementTestHelper.accountId).build();
        init = new AmazonWebServicesClientProxy(new LoggerProxy(),
            MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis()).initiate("test", proxyClient, model, callbackContext);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        doReturn(init).when(proxy).initiate(any(), any(), any(), any());
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
            ProgressEvent.progress(model, callbackContext);

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
        assertThat(response.getMessage()).containsIgnoringCase("Proactive engagement is already configured on the account.");
    }
}
