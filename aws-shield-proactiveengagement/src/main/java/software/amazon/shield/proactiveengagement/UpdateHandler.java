package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
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

        logger.log(String.format("UpdateHandler: ProactiveEngagement AccountID = %s,ClientToken = %s",
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
            .handlerName("UpdateHandler")
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
                    logger.log("UpdateHandler: early exit due to no subscription.");
                    return ProgressEvent.failed(
                        m,
                        ctx,
                        HandlerErrorCode.NotFound,
                        HandlerHelper.SUBSCRIPTION_REQUIRED_ERROR_MSG);
                }
                ctx.setSubscription(subscription);
                return null;
            })
            .build()
            .initiate()
            .then(progress -> ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    DescribeEmergencyContactSettingsRequest, DescribeEmergencyContactSettingsResponse>builder()
                .resourceType("ProactiveEngagement")
                .handlerName("UpdateHandler")
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
                        res.emergencyContactList())
                    ) {
                        logger.log("UpdateHandler: early exit due to proactive engagement not configured.");
                        return ProgressEvent.failed(
                            m,
                            ctx,
                            HandlerErrorCode.NotFound,
                            HandlerHelper.NO_PROACTIVE_ENGAGEMENT_ERROR_MSG);
                    }
                    return null;
                })
                .build()
                .initiate())
            .then(progress -> updateProactiveEngagementStatus(
                proxy, proxyClient, progress.getResourceModel(), progress.getCallbackContext(), logger
            ))
            .then(progress -> HandlerHelper.updateEmergencyContactSettings(
                "UpdateHandler",
                proxy,
                proxyClient,
                progress.getResourceModel(),
                progress.getCallbackContext(),
                logger))
            .then(progress -> {
                logger.log("Succeed handling update request.");
                return ProgressEvent.defaultSuccessHandler(progress.getResourceModel());
            });
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateProactiveEngagementStatus(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        if (ProactiveEngagementStatus.ENABLED.toString().equalsIgnoreCase(model.getProactiveEngagementStatus())) {
            return HandlerHelper.enableProactiveEngagement("UpdateHandler", proxy, proxyClient, model, context, logger);
        }
        return HandlerHelper.disableProactiveEngagement("UpdateHandler", proxy, proxyClient, model, context, logger);
    }
}
