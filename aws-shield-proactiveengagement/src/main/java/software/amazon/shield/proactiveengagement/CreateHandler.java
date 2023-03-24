package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateProactiveEngagementDetailsRequest;
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
import software.amazon.shield.proactiveengagement.helper.EventualConsistencyHandlerHelper;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class CreateHandler extends BaseHandlerStd {

    public CreateHandler() {
        super();
    }

    public CreateHandler(
            ShieldClient shieldClient,
            EventualConsistencyHandlerHelper<ResourceModel, CallbackContext> eventualConsistencyHandlerHelper) {
        super(shieldClient, eventualConsistencyHandlerHelper);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<ShieldClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> validateInput(progress, callbackContext, request))
                .then(progress -> describeSubscription(proxy, proxyClient, model, callbackContext, logger))
                .then(progress -> associateProactiveEngagement(proxy, proxyClient, model, callbackContext, logger))
                .then(eventualConsistencyHandlerHelper::waitForChangesToPropagate)
                .then(progress -> {
                    model.setProactiveEngagementStatus(ProactiveEngagementStatus.ENABLED.toString());
                    return ProgressEvent.defaultSuccessHandler(model);
                });
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> validateInput(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            CallbackContext callbackContext,
            ResourceHandlerRequest<ResourceModel> request) {
        final ResourceModel model = request.getDesiredResourceState();
        if (model.getEmergencyContactList().isEmpty()) {
            return ProgressEvent.failed(request.getDesiredResourceState(),
                    callbackContext,
                    HandlerErrorCode.InvalidRequest,
                    "[Error] - Input validation failed due to missing at least one emergency contact");
        }
        return progress;
    }

    private ProgressEvent<ResourceModel, CallbackContext> describeSubscription(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final Logger logger
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            logger.log("Starting to describe subscription.");
            return proxy.initiate("shield::describe-subscription-in-create-handler", proxyClient, model, context)
                    .translateToServiceRequest(m -> DescribeSubscriptionRequest.builder().build())
                    .makeServiceCall((req, client) -> proxy.injectCredentialsAndInvokeV2(req,
                            shieldClient::describeSubscription))
                    .handleError((request, e, client, m, callbackContext) -> {
                        logger.log("[Error] - Caught exception during describing subscription: " + e);
                        return ProgressEvent.failed(m,
                                callbackContext,
                                ExceptionConverter.convertToErrorCode((RuntimeException) e),
                                e.getMessage());
                    })
                    .done(res -> {
                        if (HandlerHelper.doesProactiveEngagementStatusExist(res)) {
                            return createProactiveEngagementResource(proxy, proxyClient, model, context, logger);
                        }
                        logger.log("Succeed describing subscription.");
                        return ProgressEvent.progress(model, context);
                    });
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> associateProactiveEngagement(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final Logger logger
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            return proxy.initiate("shield::associate-proactive-engagement", proxyClient, model, context)
                    .translateToServiceRequest((m) -> AssociateProactiveEngagementDetailsRequest.builder()
                            .emergencyContactList(HandlerHelper.convertCFNEmergencyContactList(m.getEmergencyContactList()))
                            .build())
                    .makeServiceCall((associateProactiveEngagementRequest, client) -> proxy.injectCredentialsAndInvokeV2(
                            associateProactiveEngagementRequest,
                            shieldClient::associateProactiveEngagementDetails))
                    .stabilize((request, response, client, m, callbackContext) -> checkCreateStabilization(client))
                    .handleError((request, e, client, m, callbackContext) -> {
                        logger.log("[Error] - Caught exception during associating proactive engagement: " + e);
                        return ProgressEvent.failed(m,
                                callbackContext,
                                ExceptionConverter.convertToErrorCode((RuntimeException) e),
                                e.getMessage());
                    })
                    .done((r) -> ProgressEvent.progress(model, context));
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createProactiveEngagementResource(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final Logger logger) {
        return ProgressEvent.progress(model, context)
                .then(progress -> HandlerHelper.updateEmergencyContactSettings(proxy,
                        proxyClient,
                        model,
                        context,
                        logger))
                .then(progress -> HandlerHelper.enableProactiveEngagement(proxy, proxyClient, model, context, logger))
                .then(eventualConsistencyHandlerHelper::waitForChangesToPropagate)
                .then(progress -> ProgressEvent.defaultSuccessHandler(model));
    }

    private boolean checkCreateStabilization(final ProxyClient<ShieldClient> proxyClient) {
        DescribeSubscriptionRequest describeSubscriptionRequest = DescribeSubscriptionRequest.builder().build();
        DescribeEmergencyContactSettingsRequest describeEmergencyContactSettingsRequest =
                DescribeEmergencyContactSettingsRequest.builder()
                        .build();
        DescribeSubscriptionResponse describeSubscriptionResponse;
        DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse;
        try (ShieldClient shieldClient = proxyClient.client()) {
            describeSubscriptionResponse = proxyClient.injectCredentialsAndInvokeV2(describeSubscriptionRequest,
                    shieldClient::describeSubscription);
            describeEmergencyContactSettingsResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeEmergencyContactSettingsRequest,
                    shieldClient::describeEmergencyContactSettings);
        } catch (RuntimeException e) {
            return false;
        }
        return HandlerHelper.isProactiveEngagementEnabled(describeEmergencyContactSettingsResponse,
                describeSubscriptionResponse);
    }
}
