package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateProactiveEngagementDetailsRequest;
import software.amazon.awssdk.services.shield.model.AssociateProactiveEngagementDetailsResponse;
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


public class CreateHandler extends BaseHandlerStd {

    public CreateHandler() {
        super();
    }

    public CreateHandler(ShieldClient shieldClient) {
        super(shieldClient);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<ShieldClient> proxyClient,
        final Logger logger) {

        logger.log(String.format("CreateHandler: ProactiveEngagement AccountID = %s, ClientToken = %s",
            request.getAwsAccountId(),
            request.getClientRequestToken()));
        final ResourceModel model = request.getDesiredResourceState();
        model.setAccountId(request.getAwsAccountId());

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DescribeSubscriptionRequest,
                DescribeSubscriptionResponse>builder()
            .resourceType("ProactiveEngagement")
            .handlerName("CreateHandler")
            .apiName("describeSubscription")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> DescribeSubscriptionRequest.builder().build())
            .getRequestFunction(c -> c::describeSubscription)
            .onSuccess((req, res, c, m, ctx) -> {
                final Subscription subscription = res.subscription();
                if (subscription == null) {
                    logger.log("CreateHandler: early exit due to no subscription.");
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
            .initiate()
            .then(progress -> ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    DescribeEmergencyContactSettingsRequest, DescribeEmergencyContactSettingsResponse>builder()
                .resourceType("ProactiveEngagement")
                .handlerName("CreateHandler")
                .apiName("describeEmergencyContactSettings")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(progress.getResourceModel())
                .context(progress.getCallbackContext())
                .logger(logger)
                .translateToServiceRequest(m -> DescribeEmergencyContactSettingsRequest.builder().build())
                .getRequestFunction(c -> c::describeEmergencyContactSettings)
                .onSuccess((req, res, c, m, ctx) -> {
                    if (HandlerHelper.isProactiveEngagementConfigured(ctx.getSubscription(),
                        res.emergencyContactList())) {
                        logger.log("CreateHandler: early exit due to proactive engagement already configured.");
                        return ProgressEvent.failed(
                            m,
                            ctx,
                            HandlerErrorCode.ResourceConflict,
                            HandlerHelper.PROACTIVE_ENGAGEMENT_ALREADY_CONFIGURED_ERROR_MSG);
                    }
                    return null;
                })
                .build()
                .initiate())
            .then(progress -> {
                if (progress.getCallbackContext().getSubscription().proactiveEngagementStatus() == null) {
                    return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                            AssociateProactiveEngagementDetailsRequest, AssociateProactiveEngagementDetailsResponse>builder()
                        .resourceType("ProactiveEngagement")
                        .handlerName("CreateHandler")
                        .apiName("associateProactiveEngagementDetails")
                        .proxy(proxy)
                        .proxyClient(proxyClient)
                        .model(progress.getResourceModel())
                        .context(progress.getCallbackContext())
                        .logger(logger)
                        .translateToServiceRequest(m -> AssociateProactiveEngagementDetailsRequest.builder()
                            .emergencyContactList(HandlerHelper.convertCFNEmergencyContactList(m.getEmergencyContactList()))
                            .build())
                        .getRequestFunction(c -> c::associateProactiveEngagementDetails)
                        .stabilize(HandlerHelper::stabilizeProactiveEngagementStatus)
                        .build()
                        .initiate();

                } else {
                    return reconfigProactiveEngagement(proxy,
                        proxyClient,
                        progress.getResourceModel(),
                        progress.getCallbackContext(),
                        logger);
                }
            })
            .then(progress -> {
                logger.log(String.format("Succeed handling create request: %s",
                    progress.getResourceModel().getAccountId()));
                return ProgressEvent.defaultSuccessHandler(progress.getResourceModel());
            });
    }

    private ProgressEvent<ResourceModel, CallbackContext> reconfigProactiveEngagement(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger) {
        return ProgressEvent.defaultInProgressHandler(context, 0, model)
            .then(progress -> HandlerHelper.updateEmergencyContactSettings(
                "CreateHandler",
                proxy,
                proxyClient,
                model,
                context,
                logger))
            .then(progress -> HandlerHelper.enableProactiveEngagement(
                "CreateHandler",
                proxy,
                proxyClient,
                model,
                context,
                logger));
    }
}
