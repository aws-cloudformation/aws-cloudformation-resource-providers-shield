package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;
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

        logger.log(String.format("DeleteHandler: ProactiveEngagement AccountID = %s, ClientToken = %s",
            request.getAwsAccountId(),
            request.getClientRequestToken()));
        if (!HandlerHelper.callerAccountIdMatchesResourcePrimaryId(request)) {
            return ProgressEvent.failed(request.getDesiredResourceState(),
                callbackContext,
                HandlerErrorCode.NotFound,
                HandlerHelper.ACCOUNT_ID_MISMATCH_ERROR_MSG);
        }

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DescribeSubscriptionRequest,
                DescribeSubscriptionResponse>builder()
            .resourceType("ProactiveEngagement")
            .handlerName("DeleteHandler")
            .apiName("describeSubscription")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(request.getDesiredResourceState())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> DescribeSubscriptionRequest.builder().build())
            .getRequestFunction(c -> c::describeSubscription)
            .onSuccess((req, res, c, m, ctx) -> {
                final Subscription subscription = res.subscription();
                if (subscription == null) {
                    logger.log("DeleteHandler: early exit due to no subscription.");
                    return ProgressEvent.failed(
                        m,
                        ctx,
                        HandlerErrorCode.InvalidRequest,
                        HandlerHelper.SUBSCRIPTION_REQUIRED_ERROR_MSG);
                }
                ctx.setSubscription(subscription);
                return null;
            })
            .build()
            .initiate().then(progress -> ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    DescribeEmergencyContactSettingsRequest, DescribeEmergencyContactSettingsResponse>builder()
                .resourceType("ProactiveEngagement")
                .handlerName("DeleteHandler")
                .apiName("describeEmergencyContactSettings")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(progress.getResourceModel())
                .context(progress.getCallbackContext())
                .logger(logger)
                .translateToServiceRequest(m -> DescribeEmergencyContactSettingsRequest.builder().build())
                .getRequestFunction(c -> c::describeEmergencyContactSettings)
                .onSuccess((req, res, c, m, ctx) -> {
                    if (!HandlerHelper.isProactiveEngagementConfigured(
                        ctx.getSubscription(),
                        res.emergencyContactList()
                    )) {
                        logger.log("DeleteHandler: early exit due to proactive engagement is not configured.");
                        return ProgressEvent.failed(request.getDesiredResourceState(),
                            ctx,
                            HandlerErrorCode.NotFound,
                            HandlerHelper.NO_PROACTIVE_ENGAGEMENT_ERROR_MSG);
                    }
                    return null;
                })
                .build()
                .initiate())
            .then(progress -> HandlerHelper.disableProactiveEngagement(
                "DeleteHandler",
                proxy,
                proxyClient,
                progress.getResourceModel(),
                progress.getCallbackContext(),
                logger)
            )
            .then(progress -> HandlerHelper.updateEmergencyContactSettings(
                "DeleteHandler",
                proxy,
                proxyClient,
                progress.getResourceModel(),
                progress.getCallbackContext(),
                logger))
            .then(progress -> {
                logger.log("Successfully disabled ProactiveEngagement.");
                return ProgressEvent.defaultSuccessHandler(progress.getResourceModel());
            });
    }
}
