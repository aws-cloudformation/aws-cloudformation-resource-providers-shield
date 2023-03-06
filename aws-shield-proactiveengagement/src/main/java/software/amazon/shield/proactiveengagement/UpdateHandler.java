package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.awssdk.services.shield.model.UpdateEmergencyContactSettingsRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public UpdateHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        try {
            final UpdateEmergencyContactSettingsRequest updateEmergencyContactSettingsRequest =
                    UpdateEmergencyContactSettingsRequest.builder()
                            .emergencyContactList(HandlerHelper.convertCFNEmergencyContactList(model.getEmergencyContacts()))
                            .build();
            proxy.injectCredentialsAndInvokeV2(updateEmergencyContactSettingsRequest,
                    client::updateEmergencyContactSettings);
            HandlerHelper.disableProactiveEngagement(proxy, client);
            if (model.getProactiveEngagementStatus().equalsIgnoreCase(ProactiveEngagementStatus.ENABLED.toString())) {
                HandlerHelper.enableProactiveEngagement(proxy, client);
            }

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (RuntimeException e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(ExceptionConverter.convertToErrorCode(e))
                    .message(e.getMessage())
                    .build();
        }
    }
}
