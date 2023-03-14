package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.proactiveengagement.helper.BaseHandlerStd;
import software.amazon.shield.proactiveengagement.helper.EventualConsistencyHandlerHelper;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;
import software.amazon.shield.proactiveengagement.helper.ReadHandlerHelper;

public class ReadHandler extends BaseHandlerStd {

    public ReadHandler() {
        super();
    }

    public ReadHandler(
            ShieldClient shieldClient,
            EventualConsistencyHandlerHelper<ResourceModel, CallbackContext> eventualConsistencyHandlerHelper) {
        super(shieldClient, eventualConsistencyHandlerHelper);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<ShieldClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> validateInput(progress, callbackContext, request))
                .then(progress -> HandlerHelper.describeSubscription(proxy, proxyClient, model, callbackContext))
                .then(progress -> ReadHandlerHelper.describeEmergencyContactSettings(proxy,
                        proxyClient,
                        model,
                        callbackContext))
                .then(progress -> ProgressEvent.defaultSuccessHandler(model));
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> validateInput(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final CallbackContext callbackContext,
            ResourceHandlerRequest<ResourceModel> request) {
        if (!HandlerHelper.callerAccountIdMatchesResourcePrimaryId(request)) {
            return ProgressEvent.failed(request.getDesiredResourceState(),
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    HandlerHelper.PROACTIVE_ENGAGEMENT_ACCOUNT_ID_NOT_FOUND_ERROR_MSG);
        }
        return progress;
    }
}
