package software.amazon.shield.protectiongroup;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DeleteProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.DeleteProtectionGroupResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.HandlerHelper;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;

@RequiredArgsConstructor
public class DeleteHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient shieldClient;

    public DeleteHandler() {
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
            "DeleteHandler: ProtectionGroupArn = %s, ClientToken = %s",
            request.getDesiredResourceState().getProtectionGroupArn(),
            request.getClientRequestToken()
        ));
        logger.log(String.format(
            "DeleteHandler: ProtectionGroupId = %s, ClientToken = %s",
            HandlerHelper.protectionArnToId(request.getDesiredResourceState().getProtectionGroupArn()),
            request.getClientRequestToken()
        ));
        final ProxyClient<ShieldClient> proxyClient = proxy.newProxy(() -> this.shieldClient);

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DeleteProtectionGroupRequest,
                DeleteProtectionGroupResponse>builder()
            .resourceType("ProtectionGroup")
            .handlerName("DeleteHandler")
            .apiName("deleteProtectionGroup")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(request.getDesiredResourceState())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> DeleteProtectionGroupRequest.builder()
                .protectionGroupId(HandlerHelper.protectionArnToId(m.getProtectionGroupArn()))
                .build())
            .getRequestFunction(c -> c::deleteProtectionGroup)
            .onSuccess((req, res, c, m, ctx) -> ProgressEvent.defaultSuccessHandler(m))
            .build().initiate();
    }
}
