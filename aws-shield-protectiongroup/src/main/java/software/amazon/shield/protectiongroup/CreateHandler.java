package software.amazon.shield.protectiongroup;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.CreateProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.CreateProtectionGroupResponse;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;

@RequiredArgsConstructor
public class CreateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient shieldClient;

    public CreateHandler() {
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
            "CreateHandler: AccountID = %s, ProtectionGroupId = %s, ClientToken = %s",
            request.getAwsAccountId(),
            request.getDesiredResourceState().getProtectionGroupId(),
            request.getClientRequestToken()
        ));
        final ProxyClient<ShieldClient> proxyClient = proxy.newProxy(() -> this.shieldClient);
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, CreateProtectionGroupRequest,
                CreateProtectionGroupResponse>builder()
            .resourceType("ProtectionGroup")
            .handlerName("CreateHandler")
            .apiName("createProtectionGroup")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(request.getDesiredResourceState())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> {
                final CreateProtectionGroupRequest.Builder createProtectionGroupRequestBuilder =
                    CreateProtectionGroupRequest.builder()
                        .protectionGroupId(m.getProtectionGroupId())
                        .aggregation(m.getAggregation())
                        .pattern(m.getPattern());

                if (m.getPattern().equals("ARBITRARY")) {
                    createProtectionGroupRequestBuilder.members(m.getMembers());
                } else if (m.getPattern().equals("BY_RESOURCE_TYPE")) {
                    createProtectionGroupRequestBuilder.resourceType(m.getResourceType());
                }
                if (!CollectionUtils.isNullOrEmpty(m.getTags())) {
                    createProtectionGroupRequestBuilder.tags(
                        m.getTags()
                            .stream()
                            .map(tag ->
                                Tag.builder()
                                    .key(tag.getKey())
                                    .value(tag.getValue())
                                    .build())
                            .collect(Collectors.toList())
                    );
                }
                return createProtectionGroupRequestBuilder.build();
            })
            .getRequestFunction(c -> c::createProtectionGroup)
            .onSuccess((req, res, c, m, ctx) -> {
                m.setProtectionGroupArn(String.format(
                    "arn:aws:shield::%s:protection-group/%s",
                    request.getAwsAccountId(),
                    m.getProtectionGroupId()
                ));
                return ProgressEvent.defaultSuccessHandler(m);
            })
            .build().initiate();
    }
}
