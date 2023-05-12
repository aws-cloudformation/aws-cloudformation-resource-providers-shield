package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.helper.BaseHandlerStd;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class UpdateHandler extends BaseHandlerStd {

    public UpdateHandler() {
        super();
    }

    public UpdateHandler(final ShieldClient shieldClient) {
        super(shieldClient);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<ShieldClient> proxyClient,
        final Logger logger) {

        logger.log(String.format("UpdateHandler: ProactiveEngagement AccountID = %s", request.getAwsAccountId()));
        if (!HandlerHelper.callerAccountIdMatchesResourcePrimaryId(request)) {
            return ProgressEvent.failed(request.getDesiredResourceState(),
                callbackContext,
                HandlerErrorCode.NotFound,
                HandlerHelper.ACCOUNT_ID_MISMATCH_ERROR_MSG);
        }

        final ResourceModel desiredState = request.getDesiredResourceState();

        try {
            final DescribeSubscriptionResponse describeSubscriptionResponse = proxy.injectCredentialsAndInvokeV2(
                DescribeSubscriptionRequest.builder().build(),
                shieldClient::describeSubscription);

            if (describeSubscriptionResponse.subscription() == null) {
                return ProgressEvent.failed(
                    ResourceModel.builder().accountId(request.getAwsAccountId()).build(),
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
                return ProgressEvent.failed(
                    ResourceModel.builder().accountId(request.getAwsAccountId()).build(),
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    HandlerHelper.NO_PROACTIVE_ENGAGEMENT_ERROR_MSG);
            }

            return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> updateProactiveEngagementStatus(
                    proxy, proxyClient, desiredState, callbackContext, logger
                ))
                .then(progress -> HandlerHelper.updateEmergencyContactSettings(proxy,
                    proxyClient,
                    desiredState,
                    callbackContext,
                    logger))
                .then(progress -> {
                    logger.log("Succeed handling update request.");
                    return ProgressEvent.defaultSuccessHandler(desiredState);
                });
        } catch (RuntimeException e) {
            logger.log("[ERROR] update ProactiveEngagement failed " + e);
            return ProgressEvent.failed(
                desiredState,
                callbackContext,
                ExceptionConverter.convertToErrorCode(e),
                e.getMessage());
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateProactiveEngagementStatus(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        if (ProactiveEngagementStatus.ENABLED.toString().equalsIgnoreCase(model.getProactiveEngagementStatus())) {
            return HandlerHelper.enableProactiveEngagement(proxy, proxyClient, model, context, logger);
        }
        return HandlerHelper.disableProactiveEngagement(proxy, proxyClient, model, context, logger);
    }
}
