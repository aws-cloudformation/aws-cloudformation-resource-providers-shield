package software.amazon.shield.proactiveengagement;

import java.util.Collections;

import com.google.common.collect.ImmutableList;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;
import software.amazon.shield.proactiveengagement.helper.BaseHandlerStd;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class ListHandler extends BaseHandlerStd {

    public ListHandler() {
        super();
    }

    public ListHandler(ShieldClient shieldClient) {
        super(shieldClient);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<ShieldClient> proxyClient,
        final Logger logger) {
        logger.log(String.format("ListHandler: ProactiveEngagement AccountID = %s, ClientToken = %s",
            request.getAwsAccountId(),
            request.getClientRequestToken()));

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DescribeSubscriptionRequest,
                DescribeSubscriptionResponse>builder()
            .resourceType("ProactiveEngagement")
            .handlerName("ListHandler")
            .apiName("describeSubscription")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(ResourceModel.builder().build())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> DescribeSubscriptionRequest.builder().build())
            .getRequestFunction(c -> c::describeSubscription)
            .onSuccess((req, res, c, m, ctx) -> {
                final Subscription subscription = res.subscription();
                if (subscription == null) {
                    logger.log("ListHandler: early exit due to no subscription.");
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(Collections.emptyList())
                        .status(OperationStatus.SUCCESS)
                        .build();
                }
                ctx.setSubscription(subscription);
                return null;
            })
            .build()
            .initiate()
            .then(progress -> ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    DescribeEmergencyContactSettingsRequest, DescribeEmergencyContactSettingsResponse>builder()
                .resourceType("ProactiveEngagement")
                .handlerName("ListHandler")
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
                        logger.log("ListHandler: early exit due to proactive engagement not configured.");
                        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModels(Collections.emptyList())
                            .status(OperationStatus.SUCCESS)
                            .build();
                    }
                    return null;
                })
                .build()
                .initiate())
            .then(progress -> {
                final ResourceModel model = progress.getResourceModel();
                model.setAccountId(request.getAwsAccountId());
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(ImmutableList.of(model))
                    .status(OperationStatus.SUCCESS)
                    .build();
            });
    }
}
