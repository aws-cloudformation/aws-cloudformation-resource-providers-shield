package software.amazon.shield.protection;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionResponse;
import software.amazon.awssdk.services.shield.model.Protection;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.HandlerHelper;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;

@RequiredArgsConstructor
public class ReadHandler extends BaseHandler<CallbackContext> {

    private static final String HEALTH_CHECK_ARN_TEMPLATE = "arn:aws:route53:::healthcheck/";

    private final ShieldClient shieldClient;

    public ReadHandler() {
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
                "ReadHandler: ProtectionArn = %s, ClientToken = %s",
                request.getDesiredResourceState().getProtectionArn(),
                request.getClientRequestToken()
            )
        );
        logger.log(String.format(
                "ReadHandler: ProtectionId = %s, ClientToken = %s",
                HandlerHelper.protectionArnToId(request.getDesiredResourceState().getProtectionArn()),
                request.getClientRequestToken()
            )
        );
        final ProxyClient<ShieldClient> proxyClient = proxy.newProxy(() -> this.shieldClient);
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;

        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DescribeProtectionRequest,
                DescribeProtectionResponse>builder()
            .resourceType("Protection")
            .handlerName("ReadHandler")
            .apiName("describeProtection")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(request.getDesiredResourceState())
            .context(callbackContext)
            .logger(logger)
            .translateToServiceRequest(m -> DescribeProtectionRequest.builder()
                .protectionId(HandlerHelper.protectionArnToId(m.getProtectionArn()))
                .build())
            .getRequestFunction(c -> c::describeProtection)
            .onSuccess((req, res, c, m, ctx) -> ProgressEvent.progress(
                transformToModel(res.protection()),
                ctx
            ))
            .build()
            .initiate()
            .then(progress -> {
                final ResourceModel m = progress.getResourceModel();
                return HandlerHelper.getTagsChainable(
                    m.getProtectionArn(),
                    tag ->
                        Tag.builder()
                            .key(tag.key())
                            .value(tag.value())
                            .build(),
                    "Protection",
                    "ReadHandler",
                    proxy,
                    proxyClient,
                    m,
                    progress.getCallbackContext(),
                    logger
                );
            }).then(
                progress -> {
                    final ResourceModel m = progress.getResourceModel();
                    final List<Tag> tags = progress.getCallbackContext().getTags();
                    if (tags.size() > 0) {
                        m.setTags(tags);
                    }
                    return ProgressEvent.defaultSuccessHandler(m);
                }
            );
    }

    private ResourceModel transformToModel(
        @NonNull final Protection protection
    ) {
        final List<String> healthCheckArns = protection.healthCheckIds()
            .stream()
            .map(x -> HEALTH_CHECK_ARN_TEMPLATE + x)
            .collect(Collectors.toList());

        return ResourceModel.builder()
            .protectionId(protection.id())
            .name(protection.name())
            .protectionArn(protection.protectionArn())
            .resourceArn(protection.resourceArn())
            .healthCheckArns(healthCheckArns.size() > 0 ? healthCheckArns : null)
            .applicationLayerAutomaticResponseConfiguration(
                translateAppLayerAutoResponseConfig(protection))
            .build();
    }

    private static ApplicationLayerAutomaticResponseConfiguration translateAppLayerAutoResponseConfig(
        @NonNull Protection protection
    ) {
        final software.amazon.awssdk.services.shield.model.ApplicationLayerAutomaticResponseConfiguration config =
            protection.applicationLayerAutomaticResponseConfiguration();
        if (config == null) {
            return null;
        }
        return ApplicationLayerAutomaticResponseConfiguration.builder()
            .action(config.action().block() != null
                ? Action.builder().block(Maps.newHashMap()).build()
                : Action.builder().count(Maps.newHashMap()).build()
            )
            .status(config.statusAsString())
            .build();

    }
}
