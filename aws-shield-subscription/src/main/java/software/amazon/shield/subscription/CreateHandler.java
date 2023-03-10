package software.amazon.shield.subscription;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.CreateSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.Constants;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.subscription.helper.ReadSubscriptionHelper;

@RequiredArgsConstructor
public class CreateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public CreateHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if (model.getAutoRenew().equals(Constants.DISABLED)) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.InvalidRequest)
                    .message("AutoRenew should be set to ENABLED to create subscription.")
                    .build();
        }

        try {
            proxy.injectCredentialsAndInvokeV2(
                    CreateSubscriptionRequest.builder().build(),
                    this.client::createSubscription);

            final Subscription subscription = getSubscription(proxy);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(ReadSubscriptionHelper.convertSubscription(subscription))
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

    private Subscription getSubscription(final AmazonWebServicesClientProxy proxy) {

        final DescribeSubscriptionResponse describeSubscriptionResponse =
                proxy.injectCredentialsAndInvokeV2(
                        DescribeSubscriptionRequest.builder().build(),
                        this.client::describeSubscription);

        return describeSubscriptionResponse.subscription();
    }
}
