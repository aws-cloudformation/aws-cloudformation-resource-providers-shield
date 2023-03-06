package software.amazon.shield.proactiveengagement;

import java.util.Collections;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.UpdateEmergencyContactSettingsRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public DeleteHandler() {
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
            HandlerHelper.disableProactiveEngagement(proxy, client);
            final UpdateEmergencyContactSettingsRequest updateEmergencyContactSettingsRequest =
                    UpdateEmergencyContactSettingsRequest.builder()
                    .emergencyContactList(Collections.emptyList())
                    .build();
            proxy.injectCredentialsAndInvokeV2(updateEmergencyContactSettingsRequest,
                    client::updateEmergencyContactSettings);

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
