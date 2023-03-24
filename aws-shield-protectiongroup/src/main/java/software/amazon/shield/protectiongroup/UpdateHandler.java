package software.amazon.shield.protectiongroup;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.UpdateProtectionGroupRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;

@RequiredArgsConstructor
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
            final UpdateProtectionGroupRequest.Builder updateProtectionGroupRequestBuilder =
                UpdateProtectionGroupRequest.builder()
                    .protectionGroupId(model.getProtectionGroupId())
                    .aggregation(model.getAggregation())
                    .members(model.getMembers())
                    .pattern(model.getPattern())
                    .resourceType(model.getResourceType());

            if (model.getPattern().equals("ARBITRARY")) {
                updateProtectionGroupRequestBuilder.members(model.getMembers());
            } else if (model.getPattern().equals("BY_RESOURCE_TYPE")) {
                updateProtectionGroupRequestBuilder.resourceType(model.getResourceType());
            }

            proxy.injectCredentialsAndInvokeV2(updateProtectionGroupRequestBuilder.build(),
                this.client::updateProtectionGroup);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
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
