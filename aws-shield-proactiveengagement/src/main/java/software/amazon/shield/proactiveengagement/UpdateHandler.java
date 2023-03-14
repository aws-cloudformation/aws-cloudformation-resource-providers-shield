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

        final ResourceModel input = request.getDesiredResourceState();
        final ResourceModel model = HandlerHelper.copyNewModel(input);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> validateInput(progress, callbackContext, request))
                .then(progress -> HandlerHelper.describeSubscription(proxy, proxyClient, model, callbackContext))
                .then(progress -> HandlerHelper.describeEmergencyContactSettings(proxy,
                        proxyClient,
                        model,
                        callbackContext,
                        false))
                .then(progress -> HandlerHelper.updateEmergencyContactSettings(proxy,
                        proxyClient,
                        model,
                        callbackContext))
                .then(eventualConsistencyHandlerHelper::waitForChangesToPropagate)
                .then(progress -> {
                    if (disableProactiveEngagement(input)) {
                        return HandlerHelper.disableProactiveEngagement(proxy, proxyClient, model, callbackContext);
                    }
                    return HandlerHelper.enableProactiveEngagement(proxy, proxyClient, model, callbackContext);
                })
                .then(eventualConsistencyHandlerHelper::waitForChangesToPropagate)
                .then(progress -> ProgressEvent.defaultSuccessHandler(input));
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> validateInput(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            CallbackContext callbackContext,
            ResourceHandlerRequest<ResourceModel> request) {
        if (!HandlerHelper.callerAccountIdMatchesResourcePrimaryId(request)) {
            return ProgressEvent.failed(request.getDesiredResourceState(),
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    HandlerHelper.PROACTIVE_ENGAGEMENT_ACCOUNT_ID_NOT_FOUND_ERROR_MSG);
        }
        return progress;
    }

    private boolean disableProactiveEngagement(final ResourceModel input) {
        return input.getProactiveEngagementStatus().equalsIgnoreCase(ProactiveEngagementStatus.DISABLED.toString());
    }
}
