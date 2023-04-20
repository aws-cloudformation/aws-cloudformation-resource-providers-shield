package software.amazon.shield.proactiveengagement;

import java.util.Collections;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.helper.BaseHandlerStd;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class DeleteHandler extends BaseHandlerStd {

    public DeleteHandler() {
        super();
    }

    public DeleteHandler(ShieldClient shieldClient) {
        super(shieldClient);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<ShieldClient> proxyClient,
        final Logger logger) {

        logger.log("Starting to disable proactive engagement.");
        if (!HandlerHelper.callerAccountIdMatchesResourcePrimaryId(request)) {
            return ProgressEvent.failed(request.getDesiredResourceState(),
                callbackContext,
                HandlerErrorCode.NotFound,
                HandlerHelper.ACCOUNT_ID_MISMATCH_ERROR_MSG);
        }

        final ResourceModel model = request.getDesiredResourceState();

        try {
            final DescribeSubscriptionResponse describeSubscriptionResponse = proxy.injectCredentialsAndInvokeV2(
                DescribeSubscriptionRequest.builder().build(),
                shieldClient::describeSubscription);

            if (describeSubscriptionResponse.subscription() == null) {
                return ProgressEvent.failed(request.getDesiredResourceState(),
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    HandlerHelper.SUBSCRIPTION_REQUIRED_ERROR_MSG);
            }

            final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
                proxy.injectCredentialsAndInvokeV2(
                    DescribeEmergencyContactSettingsRequest.builder().build(),
                    shieldClient::describeEmergencyContactSettings);

            if (!HandlerHelper.isProactiveEngagementConfigured(
                describeSubscriptionResponse,
                describeEmergencyContactSettingsResponse
            )) {
                return ProgressEvent.failed(request.getDesiredResourceState(),
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    HandlerHelper.NO_PROACTIVE_ENGAGEMENT_ERROR_MSG);
            }

            return ProgressEvent.progress(model, callbackContext)
                .then(progress -> HandlerHelper.disableProactiveEngagement(proxy,
                    proxyClient,
                    model,
                    callbackContext,
                    logger)
                )
                .then(progress -> {
                    model.setEmergencyContactList(Collections.emptyList());
                    return HandlerHelper.updateEmergencyContactSettings(proxy,
                        proxyClient,
                        model,
                        callbackContext,
                        logger);
                })
                .then(progress -> {
                    logger.log("Succeed disabling proactive engagement.");
                    return ProgressEvent.defaultSuccessHandler(model);
                });
        } catch (RuntimeException e) {
            return ProgressEvent.failed(
                model,
                callbackContext,
                ExceptionConverter.convertToErrorCode(e),
                e.getMessage());
        }
    }
}
