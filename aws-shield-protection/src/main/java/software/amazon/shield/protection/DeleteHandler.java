package software.amazon.shield.protection;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DeleteProtectionRequest;
import software.amazon.awssdk.services.shield.model.DeleteProtectionResponse;
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
        CallbackContext callbackContext,
        final Logger logger
    ) {
        logger.log(String.format(
                "DeleteHandler: ProtectionArn = %s, ClientToken = %s",
                request.getDesiredResourceState().getProtectionArn(),
                request.getClientRequestToken()
            )
        );
        logger.log(String.format(
                "DeleteHandler: ProtectionId = %s, ClientToken = %s",
                HandlerHelper.protectionArnToId(request.getDesiredResourceState().getProtectionArn()),
                request.getClientRequestToken()
            )
        );
        final ProxyClient<ShieldClient> proxyClient = proxy.newProxy(() -> this.shieldClient);
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DeleteProtectionRequest,
                DeleteProtectionResponse>builder()
            .resourceType("Protection")
            .handlerName("DeleteHandler")
            .apiName("deleteProtection")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(request.getDesiredResourceState())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> DeleteProtectionRequest.builder()
                .protectionId(HandlerHelper.protectionArnToId(m.getProtectionArn()))
                .build())
            .getRequestFunction(c -> c::deleteProtection)
            .onSuccess((req, res, c, m, ctx) -> ProgressEvent.defaultSuccessHandler(m))
            .build()
            .initiate();
    }
}
