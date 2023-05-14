package software.amazon.shield.proactiveengagement;

import java.time.Duration;
import java.util.Collections;

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
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper.MOCK_CREDENTIALS;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Mock
    private ShieldClient shieldClient;

    private ListHandler listHandler;

    private ProxyClient<ShieldClient> proxyClient;

    private CallbackContext callbackContext;

    private ResourceModel model;

    @BeforeEach
    public void setup() {
        proxy = spy(new AmazonWebServicesClientProxy(new LoggerProxy(),
            MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis()));
        logger = mock(Logger.class);
        listHandler = new ListHandler(shieldClient);
        proxyClient = ProactiveEngagementTestHelper.MOCK_PROXY(proxy, shieldClient);
        callbackContext = new CallbackContext();
        model = ResourceModel.builder().accountId(ProactiveEngagementTestHelper.accountId).build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
            .subscription(Subscription.builder()
                .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED)
                .build())
            .build();
        doReturn(describeSubscriptionResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());
        doReturn(DescribeEmergencyContactSettingsResponse.builder().build()).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        final ResourceModel model = ResourceModel.builder()
            .accountId(ProactiveEngagementTestHelper.accountId)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(ProactiveEngagementTestHelper.accountId)
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            listHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NoProactiveEngagementSuccess() {
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
            .subscription(Subscription.builder().build())
            .build();
        doReturn(describeSubscriptionResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());
        doReturn(DescribeEmergencyContactSettingsResponse.builder().build()).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(ProactiveEngagementTestHelper.accountId)
            .desiredResourceState(ResourceModel.builder()
                .accountId(ProactiveEngagementTestHelper.accountId)
                .build())
            .nextToken("randomNextToken")
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            listHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels().size()).isEqualTo(0);
    }

    @Test
    public void handleRequest_NoProactiveEngagementContactsSuccess() {
        final DescribeSubscriptionResponse describeSubscriptionResponse = DescribeSubscriptionResponse.builder()
            .subscription(Subscription.builder()
                .proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED)
                .build())
            .build();
        doReturn(describeSubscriptionResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeSubscriptionRequest.class), any());

        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
            DescribeEmergencyContactSettingsResponse.builder()
                .emergencyContactList(Collections.emptyList())
                .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(ProactiveEngagementTestHelper.accountId)
            .desiredResourceState(ResourceModel.builder()
                .accountId(ProactiveEngagementTestHelper.accountId)
                .build())
            .nextToken("randomNextToken")
            .build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
            listHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels().size()).isEqualTo(0);
    }
}
