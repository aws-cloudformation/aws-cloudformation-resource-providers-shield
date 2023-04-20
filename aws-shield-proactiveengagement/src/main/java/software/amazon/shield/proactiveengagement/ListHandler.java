package software.amazon.shield.proactiveengagement;

import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.helper.BaseHandlerStd;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class ListHandler extends BaseHandlerStd {

    public ListHandler() {
        super();
    }

    public ListHandler(ShieldClient shieldClient) {
        super(shieldClient);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<ShieldClient> proxyClient,
        final Logger logger) {
        logger.log("Starting to list resources.");

        try {
            final List<ResourceModel> models = new ArrayList<>();
            final DescribeSubscriptionResponse describeSubscriptionResponse = proxy.injectCredentialsAndInvokeV2(
                DescribeSubscriptionRequest.builder().build(),
                shieldClient::describeSubscription);

            if (describeSubscriptionResponse.subscription() == null) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(models)
                    .status(OperationStatus.SUCCESS)
                    .build();
            }

            final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
                proxy.injectCredentialsAndInvokeV2(
                    DescribeEmergencyContactSettingsRequest.builder().build(),
                    shieldClient::describeEmergencyContactSettings);

            if (!HandlerHelper.isProactiveEngagementConfigured(
                describeSubscriptionResponse,
                describeEmergencyContactSettingsResponse
            )) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(models)
                    .status(OperationStatus.SUCCESS)
                    .build();
            }

            final ResourceModel model = ResourceModel.builder().build();
            model.setAccountId(request.getAwsAccountId());
            models.add(model);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
        } catch (RuntimeException e) {
            return ProgressEvent.failed(
                request.getDesiredResourceState(),
                callbackContext,
                ExceptionConverter.convertToErrorCode(e),
                e.getMessage());
        }
    }
}
