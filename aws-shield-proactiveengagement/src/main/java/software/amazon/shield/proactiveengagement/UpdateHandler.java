package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.proactiveengagement.helper.BaseHandlerStd;
import software.amazon.shield.proactiveengagement.helper.EventualConsistencyHandlerHelper;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class UpdateHandler extends BaseHandlerStd {

    public UpdateHandler() {
        super();
    }

    public UpdateHandler(
            final ShieldClient shieldClient,
            final EventualConsistencyHandlerHelper<ResourceModel, CallbackContext> eventualConsistencyHandlerHelper) {
        super(shieldClient, eventualConsistencyHandlerHelper);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<ShieldClient> proxyClient,
            final Logger logger) {

        logger.log("Starting to handle update request.");
        final ResourceModel input = request.getDesiredResourceState();
        final ResourceModel model = ResourceModel.builder().build();
        model.setAccountId(request.getAwsAccountId());

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> validateInput(progress, callbackContext, request))
                .then(progress -> HandlerHelper.describeSubscription(proxy,
                        proxyClient,
                        model,
                        callbackContext,
                        logger))
                .then(progress -> HandlerHelper.describeEmergencyContactSettings(proxy,
                        proxyClient,
                        model,
                        callbackContext,
                        logger))
                .then(progress -> updateProactiveEngagement(proxy, proxyClient, input, model, callbackContext, logger))
                .then(eventualConsistencyHandlerHelper::waitForChangesToPropagate)
                .then(progress -> {
                    logger.log("Succeed handling update request.");
                    return ProgressEvent.defaultSuccessHandler(input);
                });
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> validateInput(
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final CallbackContext callbackContext,
            final ResourceHandlerRequest<ResourceModel> request) {
        ResourceModel model = request.getDesiredResourceState();
        if (!HandlerHelper.callerAccountIdMatchesResourcePrimaryId(request)) {
            return ProgressEvent.failed(request.getDesiredResourceState(),
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    HandlerHelper.PROACTIVE_ENGAGEMENT_ACCOUNT_ID_NOT_FOUND_ERROR_MSG);
        }
        // If enable proactive engagement, it needs at least one emergency contact list
        if (model.getProactiveEngagementStatus().equalsIgnoreCase(ProactiveEngagementStatus.ENABLED.toString())
                && (model.getEmergencyContactList() == null || model.getEmergencyContactList().isEmpty())) {
            return ProgressEvent.failed(request.getDesiredResourceState(),
                    callbackContext,
                    HandlerErrorCode.InvalidRequest,
                    "[Error] - Invalid update request input");
        }
        return progress;
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateProactiveEngagement(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel input,
            final ResourceModel describeResult,
            final CallbackContext context,
            final Logger logger
    ) {
        if (describeResult.getProactiveEngagementStatus()
                .equalsIgnoreCase(ProactiveEngagementStatus.ENABLED.toString())) {
            return updateProactiveEngagementStatusFirst(proxy, proxyClient, input, context, logger);
        }
        return updateEmergencyContactListFirst(proxy, proxyClient, input, context, logger);
    }

    /**
     * Current proactive engagement status is *enabled*
     * Update proactive engagement status first then update emergency contact list in order to follow the rule that
     * there will be at least one emergency contact if enabling proactive engagement
     * Ex:
     *  Proactive engagement status:    enable         -> disable
     *  Emergency contact list:         [ contact1 ]   -> []
     */
    private ProgressEvent<ResourceModel, CallbackContext> updateProactiveEngagementStatusFirst(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final Logger logger
    ) {
        return ProgressEvent.progress(model, context)
                .then(progress -> {
                    String inputStatus = model.getProactiveEngagementStatus();
                    if (inputStatus.equalsIgnoreCase(ProactiveEngagementStatus.ENABLED.toString())) {
                        return progress;
                    }
                    return updateProactiveEngagement(proxy, proxyClient, model, context, logger);
                })
                .then(progress -> HandlerHelper.updateEmergencyContactSettings(proxy,
                        proxyClient,
                        model,
                        context,
                        logger));
    }

    /**
     * Current proactive engagement status is *disabled*
     * Update emergency contact list first then update proactive engagement status in order to follow the rule that
     * there will be at least one emergency contact if enabling proactive engagement
     * Ex:
     *  Proactive engagement status:    disable -> enable
     *  Emergency contact list:         []      -> [ contact1 ]
     */
    private ProgressEvent<ResourceModel, CallbackContext> updateEmergencyContactListFirst(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final Logger logger
    ) {
        return ProgressEvent.progress(model, context)
                .then(progress -> HandlerHelper.updateEmergencyContactSettings(proxy,
                        proxyClient,
                        model,
                        context,
                        logger))
                .then(progress -> {
                    String inputStatus = model.getProactiveEngagementStatus();
                    if (inputStatus.equalsIgnoreCase(ProactiveEngagementStatus.DISABLED.toString())) {
                        return progress;
                    }
                    return updateProactiveEngagement(proxy, proxyClient, model, context, logger);
                });
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateProactiveEngagement(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final Logger logger
    ) {
        if (disableProactiveEngagement(model)) {
            return HandlerHelper.disableProactiveEngagement(proxy, proxyClient, model, context, logger);
        }
        return HandlerHelper.enableProactiveEngagement(proxy, proxyClient, model, context, logger);
    }

    private boolean disableProactiveEngagement(final ResourceModel model) {
        return model.getProactiveEngagementStatus().equalsIgnoreCase(ProactiveEngagementStatus.DISABLED.toString());
    }
}
