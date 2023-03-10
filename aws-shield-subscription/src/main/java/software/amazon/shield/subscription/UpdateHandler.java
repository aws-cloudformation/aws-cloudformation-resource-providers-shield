package software.amazon.shield.subscription;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.UpdateSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.UpdateSubscriptionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
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

            UpdateSubscriptionRequest updateSubscriptionRequest =
                    UpdateSubscriptionRequest.builder()
                            .autoRenew(model.getSubscriptionArn())
                            .build();

            proxy.injectCredentialsAndInvokeV2(updateSubscriptionRequest, this.client::updateSubscription);

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
