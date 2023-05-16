package software.amazon.shield.protectiongroup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ListProtectionGroupsRequest;
import software.amazon.awssdk.services.shield.model.ListProtectionGroupsResponse;
import software.amazon.awssdk.services.shield.model.ProtectionGroup;
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
        ));
        final ProxyClient<ShieldClient> proxyClient = proxy.newProxy(() -> this.shieldClient);
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, ListProtectionGroupsRequest,
                ListProtectionGroupsResponse>builder()
            .resourceType("ProtectionGroup")
            .handlerName("ListHandler")
            .apiName("listProtectionGroups")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(request.getDesiredResourceState())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> ListProtectionGroupsRequest.builder()
                .nextToken(request.getNextToken())
                .build())
            .getRequestFunction(c -> c::listProtectionGroups)
            .onSuccess((req, res, c, m, ctx) -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(transferToModels(res.protectionGroups(), proxy))
                .status(OperationStatus.SUCCESS)
                .nextToken(res.nextToken())
                .build())
            .build().initiate();
    }

    private List<ResourceModel> transferToModels(
        final List<ProtectionGroup> protectionGroups,
        final AmazonWebServicesClientProxy proxy
    ) {
        return Optional.ofNullable(protectionGroups)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(
                protectionGroup ->
                    ResourceModel.builder()
                        .protectionGroupId(protectionGroup.protectionGroupId())
                        .protectionGroupArn(protectionGroup.protectionGroupArn())
                        .build())
            .collect(Collectors.toList());
    }
}
