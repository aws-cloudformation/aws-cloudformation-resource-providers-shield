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
import software.amazon.shield.proactiveengagement.helper.EventualConsistencyHandlerHelper;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;
import software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Mock
    private ShieldClient shieldClient;

    @Mock
    private EventualConsistencyHandlerHelper<ResourceModel, CallbackContext>
            eventualConsistencyHandlerHelper;

    private ProxyClient<ShieldClient> proxyClient;

    private ReadHandler readHandler;

    private CallbackContext callbackContext;

    private ResourceModel model;

    private CallChain.RequestMaker<ShieldClient, ResourceModel, CallbackContext> init;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        readHandler = new ReadHandler();
        proxyClient = ProactiveEngagementTestHelper.MOCK_PROXY(proxy, shieldClient);
        callbackContext = new CallbackContext();
        eventualConsistencyHandlerHelper = new EventualConsistencyHandlerHelper<>();
        model = ResourceModel.builder().accountId(ProactiveEngagementTestHelper.accountId).build();
        init = new AmazonWebServicesClientProxy(new LoggerProxy(),
                MOCK_CREDENTIALS,
                () -> Duration.ofSeconds(600).toMillis()).initiate("test", proxyClient, model, callbackContext);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        doReturn(init).when(proxy).initiate(any(), any(), any(), any());
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
                .subscription(Subscription.builder()
                        .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED)
                        .build())
                .build();
        doReturn(describeSubscriptionResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());

        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
                DescribeEmergencyContactSettingsResponse.builder()
                .emergencyContactList(ProactiveEngagementTestHelper.emergencyContactList)
                .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(ProactiveEngagementTestHelper.accountId)
                .desiredResourceState(model)
                .nextToken("randomNextToken")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
                .getEmergencyContactList()).isEqualTo(HandlerHelper.convertSDKEmergencyContactList(
                ProactiveEngagementTestHelper.emergencyContactList));
        assertThat(response.getResourceModel()
                .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.ENABLED.toString());
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NoProactiveEngagementFailure() {
        doReturn(init).when(proxy).initiate(any(), any(), any(), any());
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
                .build();
        doReturn(describeSubscriptionResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(ProactiveEngagementTestHelper.accountId)
                .desiredResourceState(model)
                .nextToken("randomNextToken")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
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
                readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
