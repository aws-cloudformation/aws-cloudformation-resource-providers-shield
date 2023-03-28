package software.amazon.shield.protection;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DeleteProtectionRequest;
import software.amazon.awssdk.services.shield.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.common.HandlerHelper;

@RequiredArgsConstructor
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
        final String protectionArn = model.getProtectionArn();
        logger.log(String.format("DeleteHandler: delete protection arn = %s", protectionArn));
        final String protectionId = HandlerHelper.protectionArnToId(protectionArn);
        logger.log(String.format("DeleteHandler: delete protection id = %s", protectionId));

        try {
            final DeleteProtectionRequest deleteProtectionRequest =
                DeleteProtectionRequest.builder()
                    .protectionId(protectionId)
                    .build();

            proxy.injectCredentialsAndInvokeV2(deleteProtectionRequest, this.client::deleteProtection);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.SUCCESS)
                .build();

        } catch (ResourceNotFoundException e) {
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
