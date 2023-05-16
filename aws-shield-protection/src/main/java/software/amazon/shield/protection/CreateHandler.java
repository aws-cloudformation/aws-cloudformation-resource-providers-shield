package software.amazon.shield.protection;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.BlockAction;
import software.amazon.awssdk.services.shield.model.CountAction;
import software.amazon.awssdk.services.shield.model.CreateProtectionRequest;
import software.amazon.awssdk.services.shield.model.CreateProtectionRequest.Builder;
import software.amazon.awssdk.services.shield.model.CreateProtectionResponse;
import software.amazon.awssdk.services.shield.model.DeleteProtectionRequest;
import software.amazon.awssdk.services.shield.model.DeleteProtectionResponse;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionResponse;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseResponse;
import software.amazon.awssdk.services.shield.model.ResponseAction;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.HandlerHelper;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;

import static software.amazon.shield.protection.helper.HandlerHelper.associateHealthChecks;

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
                "CreateHandler: AccountID = %s, ClientToken = %s",
                request.getAwsAccountId(),
                request.getClientRequestToken()
            )
        );
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;

        final ProxyClient<ShieldClient> proxyClient = proxy.newProxy(() -> this.shieldClient);

        final ProgressEvent<ResourceModel, CallbackContext> createProgress =
            ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, CreateProtectionRequest,
                    CreateProtectionResponse>builder()
                .resourceType("Protection")
                .handlerName("CreateHandler")
                .apiName("createProtection")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(request.getDesiredResourceState())
                .context(callbackContext)
                .logger(logger)
                .translateToServiceRequest(m -> {
                    final CreateProtectionRequest.Builder createProtectionRequestBuilder =
                        CreateProtectionRequest.builder()
                            .name(m.getName())
                            .resourceArn(m.getResourceArn());
                    populateTags(m, createProtectionRequestBuilder);
                    return createProtectionRequestBuilder.build();
                })
                .getRequestFunction(c -> c::createProtection)
                .onSuccess((req, res, c, m, ctx) -> {
                    logger.log(String.format("CreateHandler: new protection created id = %s", res.protectionId()));
                    m.setProtectionId(res.protectionId());
                    return null;
                })
                .build()
                .initiate()
                .then(progress -> ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                        DescribeProtectionRequest,
                        DescribeProtectionResponse>builder()
                    .resourceType("Protection")
                    .handlerName("CreateHandler")
                    .apiName("describeProtection")
                    .proxy(proxy)
                    .proxyClient(proxyClient)
                    .model(progress.getResourceModel())
                    .context(progress.getCallbackContext())
                    .logger(logger)
                    .translateToServiceRequest(m -> DescribeProtectionRequest.builder()
                        .protectionId(m.getProtectionId())
                        .build())
                    .getRequestFunction(c -> c::describeProtection)
                    .onSuccess((req, res, c, m, ctx) -> {
                        final String protectionArn = res.protection().protectionArn();
                        logger.log(String.format("CreateHandler: new protection created arn = %s", protectionArn));
                        m.setProtectionArn(protectionArn);
                        return null;
                    })
                    .build()
                    .initiate())
                .then(progress -> associateHealthChecks(
                    "CreateHandler",
                    progress.getResourceModel().getProtectionId(),
                    progress.getResourceModel().getHealthCheckArns(),
                    proxy,
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext(),
                    logger
                ))
                .then(progress -> {
                    final ResourceModel model = progress.getResourceModel();
                    final ApplicationLayerAutomaticResponseConfiguration appLayerAutoResponseConfig =
                        model.getApplicationLayerAutomaticResponseConfiguration();

                    if (appLayerAutoResponseConfig == null
                        || appLayerAutoResponseConfig.getStatus().equals("DISABLED")) {
                        return progress;
                    }

                    return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                            EnableApplicationLayerAutomaticResponseRequest,
                            EnableApplicationLayerAutomaticResponseResponse>builder()
                        .resourceType("Protection")
                        .handlerName("CreateHandler")
                        .apiName("enableApplicationLayerAutomaticResponse")
                        .proxy(proxy)
                        .proxyClient(proxyClient)
                        .model(model)
                        .context(progress.getCallbackContext())
                        .logger(logger)
                        .translateToServiceRequest(m -> {
                            if (m.getApplicationLayerAutomaticResponseConfiguration().getAction().getBlock() != null) {
                                return EnableApplicationLayerAutomaticResponseRequest.builder()
                                    .resourceArn(m.getResourceArn())
                                    .action(
                                        ResponseAction.builder()
                                            .block(BlockAction.builder().build())
                                            .build())
                                    .build();
                            } else {
                                return EnableApplicationLayerAutomaticResponseRequest.builder()
                                    .resourceArn(m.getResourceArn())
                                    .action(
                                        ResponseAction.builder()
                                            .count(CountAction.builder().build())
                                            .build())
                                    .build();
                            }
                        })
                        .getRequestFunction(c -> c::enableApplicationLayerAutomaticResponse)
                        .build()
                        .initiate();
                })
                .then(progress -> ProgressEvent.defaultSuccessHandler(
                    progress.getResourceModel()
                ));

        if (
            createProgress.isFailed()
                && !HandlerHelper.isRetriableErrorCode(createProgress.getErrorCode())
                && createProgress.getResourceModel().getProtectionId() != null
        ) {
            return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    DeleteProtectionRequest,
                    DeleteProtectionResponse>builder()
                .resourceType("Protection")
                .handlerName("CreateHandler")
                .apiName("deleteProtection")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(createProgress.getResourceModel())
                .context(createProgress.getCallbackContext())
                .logger(logger)
                .translateToServiceRequest(m -> DeleteProtectionRequest.builder()
                    .protectionId(m.getProtectionId())
                    .build())
                .getRequestFunction(c -> c::deleteProtection)
                .onSuccess((req, res, c, m, ctx) -> ProgressEvent.failed(
                    m,
                    ctx,
                    createProgress.getErrorCode(),
                    createProgress.getMessage()
                ))
                .build()
                .initiate();
        }
        return createProgress;
    }

    private static void populateTags(final ResourceModel model, final Builder createProtectionRequest) {

        if (!CollectionUtils.isNullOrEmpty(model.getTags())) {
            createProtectionRequest.tags(
                model.getTags()
                    .stream()
                    .map(tag ->
                        Tag.builder()
                            .key(tag.getKey())
                            .value(tag.getValue())
                            .build())
                    .collect(Collectors.toList())
            );
        }
    }
}
