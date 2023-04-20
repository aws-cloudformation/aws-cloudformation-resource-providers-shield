package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateProactiveEngagementDetailsRequest;
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
import software.amazon.shield.common.ExceptionConverter;
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

        logger.log("starting to handle create request");
        final ResourceModel model = request.getDesiredResourceState();
        model.setAccountId(request.getAwsAccountId());

        try {
            final DescribeSubscriptionResponse describeSubscriptionResponse = proxy.injectCredentialsAndInvokeV2(
                DescribeSubscriptionRequest.builder().build(),
                shieldClient::describeSubscription);
            final Subscription subscription = describeSubscriptionResponse.subscription();
            if (subscription == null) {
                logger.log("CreateHandler: early exit due to no subscription.");
                return ProgressEvent.failed(
                    model,
                    callbackContext,
                    HandlerErrorCode.InvalidRequest,
                    HandlerHelper.SUBSCRIPTION_REQUIRED_ERROR_MSG
                );
            }

            final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
                proxy.injectCredentialsAndInvokeV2(
                    DescribeEmergencyContactSettingsRequest.builder().build(),
                    shieldClient::describeEmergencyContactSettings);

            if (HandlerHelper.isProactiveEngagementConfigured(
                describeSubscriptionResponse,
                describeEmergencyContactSettingsResponse
            )) {
                logger.log("CreateHandler: early exit due to proactive engagement already configured.");
                return ProgressEvent.failed(
                    model,
                    callbackContext,
                    HandlerErrorCode.ResourceConflict,
                    HandlerHelper.PROACTIVE_ENGAGEMENT_ALREADY_CONFIGURED_ERROR_MSG
                );
            }

            return ProgressEvent.progress(model, callbackContext)
                .then(progress -> {
                    if (subscription.proactiveEngagementStatus() == null) {
                        return associateProactiveEngagement(proxy, request, proxyClient, callbackContext, logger);
                    } else {
                        return reconfigProactiveEngagement(proxy, request, proxyClient, callbackContext, logger);
                    }
                })
                .then(progress -> {
                    logger.log(String.format("Succeed handling create request: %s", model));
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

    private ProgressEvent<ResourceModel, CallbackContext> associateProactiveEngagement(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final ProxyClient<ShieldClient> proxyClient,
        final CallbackContext context,
        final Logger logger
    ) {
        final ResourceModel model = request.getDesiredResourceState();
        logger.log("Starting to associate proactive engagement");
        try (ShieldClient shieldClient = proxyClient.client()) {
            return proxy.initiate("shield::associate-proactive-engagement", proxyClient, model, context)
                .translateToServiceRequest((m) -> AssociateProactiveEngagementDetailsRequest.builder()
                    .emergencyContactList(HandlerHelper.convertCFNEmergencyContactList(m.getEmergencyContactList()))
                    .build())
                .makeServiceCall((associateProactiveEngagementRequest, client) -> proxy.injectCredentialsAndInvokeV2(
                    associateProactiveEngagementRequest,
                    shieldClient::associateProactiveEngagementDetails))
                .stabilize((r, response, client, m, c) -> HandlerHelper.stabilizeProactiveEngagementStatus(client))
                .handleError((r, e, client, m, callbackContext) -> {
                    logger.log("[Error] - Caught exception during associating proactive engagement: " + e);
                    return ProgressEvent.failed(m,
                        callbackContext,
                        ExceptionConverter.convertToErrorCode((RuntimeException) e),
                        e.getMessage());
                })
                .done((r) -> {
                    logger.log("Succeed associating proactive engagement");
                    return ProgressEvent.progress(model, context);
                });
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> reconfigProactiveEngagement(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final ProxyClient<ShieldClient> proxyClient,
        final CallbackContext context,
        final Logger logger) {
        final ResourceModel model = request.getDesiredResourceState();
        return ProgressEvent.progress(model, context)
            .then(progress -> HandlerHelper.updateEmergencyContactSettings(proxy,
                proxyClient,
                model,
                context,
                logger))
            .then(progress -> HandlerHelper.enableProactiveEngagement(proxy, proxyClient, model, context, logger));
    }
}
