package software.amazon.shield.protectiongroup;

import java.util.List;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.HandlerHelper;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;

@RequiredArgsConstructor
public class ReadHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient shieldClient;

    public ReadHandler() {
        this.shieldClient = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        logger.log(String.format("ReadHandler: ProtectionGroupArn = %s, ClientToken = %s",
            request.getDesiredResourceState().getProtectionGroupArn(),
            request.getClientRequestToken()));
        logger.log(String.format("ReadHandler: ProtectionGroupId = %s, ClientToken = %s",
            HandlerHelper.protectionArnToId(request.getDesiredResourceState().getProtectionGroupArn()),
            request.getClientRequestToken()));

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DescribeProtectionGroupRequest,
                DescribeProtectionGroupResponse>builder()
            .resourceType("ProtectionGroup")
            .handlerName("DescribeHandler")
            .apiName("describeProtectionGroup")
            .proxy(proxy)
            .proxyClient(proxy.newProxy(() -> this.shieldClient))
            .model(request.getDesiredResourceState())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> DescribeProtectionGroupRequest.builder()
                .protectionGroupId(HandlerHelper.protectionArnToId(m.getProtectionGroupArn()))
                .build())
            .getRequestFunction(c -> c::describeProtectionGroup)
            .onSuccess((req, res, c, m, ctx) -> {
                final List<Tag> tags =
                    HandlerHelper.getTags(
                        proxy,
                        c.client(),
                        res.protectionGroup().protectionGroupArn(),
                        tag -> Tag.builder()
                            .key(tag.key())
                            .value(tag.value())
                            .build());
                final ResourceModel result =
                    ResourceModel.builder()
                        .protectionGroupId(res.protectionGroup().protectionGroupId())
                        .protectionGroupArn(res.protectionGroup().protectionGroupArn())
                        .pattern(res.protectionGroup().patternAsString())
                        .members(res.protectionGroup().members())
                        .aggregation(res.protectionGroup().aggregationAsString())
                        .build();

                if (null != res.protectionGroup().resourceType()) {
                    result.setResourceType(res.protectionGroup().resourceTypeAsString());
                }
                if (!CollectionUtils.isNullOrEmpty(tags)) {
                    result.setTags(tags);
                }
                return ProgressEvent.defaultSuccessHandler(result);
            })
            .build().initiate();
    }
}
