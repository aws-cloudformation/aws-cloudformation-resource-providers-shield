package software.amazon.shield.protection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ListProtectionsRequest;
import software.amazon.awssdk.services.shield.model.ListProtectionsResponse;
import software.amazon.awssdk.services.shield.model.Protection;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;

@RequiredArgsConstructor
public class ListHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient shieldClient;

    public ListHandler() {
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
                "ListHandler: AccountID = %s, ClientToken = %s",
                request.getAwsAccountId(),
                request.getClientRequestToken()
            )
        );
        final ProxyClient<ShieldClient> proxyClient = proxy.newProxy(() -> this.shieldClient);
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, ListProtectionsRequest,
                ListProtectionsResponse>builder()
            .resourceType("Protection")
            .handlerName("ListHandler")
            .apiName("listProtections")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(request.getDesiredResourceState())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> ListProtectionsRequest.builder()
                .nextToken(request.getNextToken())
                .build())
            .getRequestFunction(c -> c::listProtections)
            .onSuccess((req, res, c, m, ctx) ->
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .resourceModels(transformToModels(res.protections()))
                    .nextToken(res.nextToken())
                    .build())
            .build()
            .initiate();
    }

    private List<ResourceModel> transformToModels(
        final List<Protection> protections
    ) {
        return Optional.ofNullable(protections)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(
                protection ->
                    ResourceModel.builder()
                        .protectionId(protection.id())
                        .name(protection.name())
                        .protectionArn(protection.protectionArn())
                        .resourceArn(protection.resourceArn())
                        .build())
            .collect(Collectors.toList());
    }
}
