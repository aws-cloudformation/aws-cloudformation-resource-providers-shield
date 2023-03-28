package software.amazon.shield.proactiveengagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.proactiveengagement.helper.BaseHandlerStd;
import software.amazon.shield.proactiveengagement.helper.EventualConsistencyHandlerHelper;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class ListHandler extends BaseHandlerStd {

    public ListHandler() {
        super();
    }

    public ListHandler(
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
        logger.log("Starting to list resources.");
        final ResourceModel model = ResourceModel.builder().build();

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
                .then(progress -> checkAccountDisabledProactiveEngagement(progress, model, logger))
                .then(progress -> {
                    final List<ResourceModel> models = new ArrayList<>();
                    model.setAccountId(request.getAwsAccountId());
                    models.add(model);
                    logger.log("Succeed listing resources.");
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModels(models)
                            .status(OperationStatus.SUCCESS)
                            .build();
                });
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> validateInput(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            CallbackContext callbackContext,
            ResourceHandlerRequest<ResourceModel> request) {
        return progress;
    }

    private ProgressEvent<ResourceModel, CallbackContext> checkAccountDisabledProactiveEngagement(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            ResourceModel model,
            Logger logger) {
        if (model.getProactiveEngagementStatus().equalsIgnoreCase(ProactiveEngagementStatus.DISABLED.toString())
                && (model.getEmergencyContactList() == null || model.getEmergencyContactList().isEmpty())) {
            logger.log(String.format("[Error] - %s", HandlerHelper.NO_PROACTIVE_ENGAGEMENT_ERROR_MSG));
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Collections.emptyList())
                    .status(OperationStatus.SUCCESS)
                    .build();
        }
        return progress;
    }
}
