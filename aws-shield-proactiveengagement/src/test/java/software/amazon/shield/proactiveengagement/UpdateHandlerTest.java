package software.amazon.shield.proactiveengagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.model.DisableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.DisableProactiveEngagementResponse;
import software.amazon.awssdk.services.shield.model.EnableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.EnableProactiveEngagementResponse;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.awssdk.services.shield.model.UpdateEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.UpdateEmergencyContactSettingsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.proactiveengagement.helper.ProactiveEngagementTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private UpdateHandler updateHandler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        updateHandler = new UpdateHandler();
    }

    @Test
    public void handleRequest_EnableProactiveEngagement() {
        final UpdateHandler handler = new UpdateHandler();

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

        final EnableProactiveEngagementResponse enableProactiveEngagementResponse =
                EnableProactiveEngagementResponse.builder()
                        .build();
        doReturn(enableProactiveEngagementResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(EnableProactiveEngagementRequest.class), any());

        final ResourceModel model = ResourceModel.builder()
                .accountId(ProactiveEngagementTestHelper.accountId)
                .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED.toString())
                .emergencyContacts(ProactiveEngagementTestHelper.convertEmergencyContactList(
                        ProactiveEngagementTestHelper.emergencyContactList))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
                .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.ENABLED.toString());
        assertThat(response.getResourceModel()
                .getEmergencyContacts()).isEqualTo(ProactiveEngagementTestHelper.convertEmergencyContactList(
                ProactiveEngagementTestHelper.emergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DisableProactiveEngagement() {
        final UpdateHandler handler = new UpdateHandler();

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

        final ResourceModel model = ResourceModel.builder()
                .accountId(ProactiveEngagementTestHelper.accountId)
                .proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED.toString())
                .emergencyContacts(ProactiveEngagementTestHelper.convertEmergencyContactList(
                        ProactiveEngagementTestHelper.emergencyContactList))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()
                .getProactiveEngagementStatus()).isEqualTo(ProactiveEngagementStatus.DISABLED.toString());
        assertThat(response.getResourceModel()
                .getEmergencyContacts()).isEqualTo(ProactiveEngagementTestHelper.convertEmergencyContactList(
                ProactiveEngagementTestHelper.emergencyContactList));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
