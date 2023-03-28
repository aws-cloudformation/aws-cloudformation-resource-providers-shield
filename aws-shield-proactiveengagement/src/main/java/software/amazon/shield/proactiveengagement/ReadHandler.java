package software.amazon.shield.proactiveengagement;

import java.util.Collections;

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

        logger.log("Starting to handle read request.");
        final ResourceModel result = ResourceModel.builder().build();
        result.setAccountId(request.getAwsAccountId());

        return ProgressEvent.progress(result, callbackContext)
                .then(progress -> validateInput(progress, callbackContext, request))
                .then(progress -> HandlerHelper.describeSubscription(proxy,
                        proxyClient,
                        result,
                        callbackContext,
                        logger))
                .then(progress -> HandlerHelper.describeEmergencyContactSettings(proxy,
                        proxyClient,
                        result,
                        callbackContext,
                        logger))
                .then(progress -> HandlerHelper.checkAccountDisabledProactiveEngagement(progress,
                        callbackContext,
                        request,
                        result,
                        logger))
                .then(progress -> {
                    logger.log("Succeed handling read request.");
                    return ProgressEvent.defaultSuccessHandler(result);
                });
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> validateInput(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final CallbackContext callbackContext,
            ResourceHandlerRequest<ResourceModel> request) {
        if (!HandlerHelper.callerAccountIdMatchesResourcePrimaryId(request)) {
            return ProgressEvent.failed(ResourceModel.builder()
                            .accountId(request.getAwsAccountId())
                            .emergencyContactList(Collections.emptyList())
                            .proactiveEngagementStatus(ProactiveEngagementStatus.DISABLED.toString())
                            .build(),
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    HandlerHelper.PROACTIVE_ENGAGEMENT_ACCOUNT_ID_NOT_FOUND_ERROR_MSG);
        }
        return progress;
    }
}
