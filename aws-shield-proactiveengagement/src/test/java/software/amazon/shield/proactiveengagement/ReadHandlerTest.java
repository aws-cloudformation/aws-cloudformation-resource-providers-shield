package software.amazon.shield.proactiveengagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;
import software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ReadHandler readHandler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        readHandler = new ReadHandler();
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

        final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
                DescribeEmergencyContactSettingsResponse.builder()
                        .emergencyContactList(ProactiveEngagementTestHelper.emergencyContactList)
                        .build();
        doReturn(describeEmergencyContactSettingsResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeEmergencyContactSettingsRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder()
                        .accountId(ProactiveEngagementTestHelper.accountId)
                        .build())
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = readHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
                .getEmergencyContacts()).isEqualTo(HandlerHelper.convertSDKEmergencyContactList(
                ProactiveEngagementTestHelper.emergencyContactList));
        assertThat(response.getResourceModel()
                .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.ENABLED.toString());
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
