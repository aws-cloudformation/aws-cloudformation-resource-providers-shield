package software.amazon.shield.protection;

import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionResponse;
import software.amazon.awssdk.services.shield.model.Protection;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.common.HandlerHelper;

@RequiredArgsConstructor
public class ReadHandler extends BaseHandler<CallbackContext> {

    private static final String HEALTH_CHECK_ARN_TEMPLATE = "arn:aws:route53:::healthcheck/";

    private final ShieldClient client;

    public ReadHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final String protectionArn = model.getProtectionArn();
        logger.log(String.format("ReadHandler: protection arn = %s", protectionArn));
        final String protectionId = HandlerHelper.protectionArnToId(protectionArn);
        logger.log(String.format("ReadHandler: protection id = %s", protectionId));

        try {
            final DescribeProtectionRequest describeProtectionRequest =
                DescribeProtectionRequest.builder()
                    .protectionId(protectionId)
                    .build();

            final DescribeProtectionResponse describeProtectionResponse =
                proxy.injectCredentialsAndInvokeV2(describeProtectionRequest, this.client::describeProtection);

            return ProgressEvent.defaultSuccessHandler(
                transformToModel(describeProtectionResponse.protection(), proxy)
            );
        } catch (RuntimeException e) {
            logger.log(String.format("ReadHandler: error %s", e));
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(ExceptionConverter.convertToErrorCode(e))
                .message(e.getMessage())
                .build();
        }
    }

    private ResourceModel transformToModel(
        @NonNull final Protection protection,
        @NonNull final AmazonWebServicesClientProxy proxy) {

        final List<String> healthCheckArns = protection.healthCheckIds()
            .stream()
            .map(x -> HEALTH_CHECK_ARN_TEMPLATE + x)
            .collect(Collectors.toList());
        final List<Tag> tags = HandlerHelper.getTags(
            proxy,
            this.client,
            protection.protectionArn(),
            tag ->
                Tag.builder()
                    .key(tag.key())
                    .value(tag.value())
                    .build());

        return ResourceModel.builder()
            .protectionId(protection.id())
            .name(protection.name())
            .protectionArn(protection.protectionArn())
            .resourceArn(protection.resourceArn())
            .tags(tags.size() > 0 ? tags : null)
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
                ? Action.builder().block(Block.builder().build()).build()
                : Action.builder().count(Count.builder().build()).build()
            )
            .status(config.statusAsString())
            .build();

    }
}
