package software.amazon.shield.proactiveengagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.model.AssociateProactiveEngagementDetailsRequest;
import software.amazon.awssdk.services.shield.model.AssociateProactiveEngagementDetailsResponse;
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
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler createHandler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        createHandler = new CreateHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final AssociateProactiveEngagementDetailsResponse associateProactiveEngagementDetailsResponse =
                AssociateProactiveEngagementDetailsResponse.builder()
                .build();

        doReturn(associateProactiveEngagementDetailsResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(AssociateProactiveEngagementDetailsRequest.class), any());

        final ResourceModel model = ResourceModel.builder()
                .accountId(ProactiveEngagementTestHelper.accountId)
                .emergencyContacts(HandlerHelper.convertSDKEmergencyContactList(ProactiveEngagementTestHelper.emergencyContactList))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = createHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
                .getEmergencyContacts()).isEqualTo(HandlerHelper.convertSDKEmergencyContactList(ProactiveEngagementTestHelper.emergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
