package software.amazon.shield.protectiongroup;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.UpdateProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.UpdateProtectionGroupResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.HandlerHelper;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;

@RequiredArgsConstructor
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient shieldClient;

    public UpdateHandler() {
        this.shieldClient = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger
    ) {

        logger.log(String.format(
            "UpdateHandler: ProtectionGroupArn = %s, ClientToken = %s",
            request.getDesiredResourceState().getProtectionGroupArn(),
            request.getClientRequestToken()
        ));
        logger.log(String.format(
            "UpdateHandler: ProtectionGroupId = %s, ClientToken = %s",
            HandlerHelper.protectionArnToId(request.getDesiredResourceState().getProtectionGroupArn()),
            request.getClientRequestToken()
        ));
        final ProxyClient<ShieldClient> proxyClient = proxy.newProxy(() -> this.shieldClient);

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, UpdateProtectionGroupRequest,
                UpdateProtectionGroupResponse>builder()
            .resourceType("ProtectionGroup")
            .handlerName("UpdateHandler")
            .apiName("updateProtectionGroup")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(request.getDesiredResourceState())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> {
                final UpdateProtectionGroupRequest.Builder updateProtectionGroupRequestBuilder =
                    UpdateProtectionGroupRequest.builder()
                        .protectionGroupId(HandlerHelper.protectionArnToId(m.getProtectionGroupArn()))
                        .aggregation(m.getAggregation())
                        .pattern(m.getPattern());

                if (m.getPattern().equals("ARBITRARY")) {
                    updateProtectionGroupRequestBuilder.members(m.getMembers());
                } else if (m.getPattern().equals("BY_RESOURCE_TYPE")) {
                    updateProtectionGroupRequestBuilder.resourceType(m.getResourceType());
                }

                return updateProtectionGroupRequestBuilder.build();
            })
            .getRequestFunction(c -> c::updateProtectionGroup)
            .build()
            .initiate()
            .then(progress -> HandlerHelper.updateTagsChainable(
                progress.getResourceModel().getTags(),
                Tag::getKey,
                Tag::getValue,
                request.getPreviousResourceState().getTags(),
                Tag::getKey,
                Tag::getValue,
                progress.getResourceModel().getProtectionGroupArn(),
                "ProtectionGroup",
                "UpdateHandler",
                proxy,
                proxyClient,
                progress.getResourceModel(),
                progress.getCallbackContext(),
                logger
            ))
            .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }
}
