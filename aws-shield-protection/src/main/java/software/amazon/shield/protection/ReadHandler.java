package software.amazon.shield.protection;

import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionResponse;
import software.amazon.awssdk.services.shield.model.Protection;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
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
        logger.log(String.format("ReadHandler: %s", protectionArn));
        final String protectionId = HandlerHelper.protectionArnToId(protectionArn);

        try {
            final DescribeProtectionRequest describeProtectionRequest =
                    DescribeProtectionRequest.builder()
                            .protectionId(protectionId)
                            .build();

            final DescribeProtectionResponse describeProtectionResponse =
                    proxy.injectCredentialsAndInvokeV2(describeProtectionRequest, this.client::describeProtection);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(translateResponse(describeProtectionResponse, proxy))
                    .status(OperationStatus.SUCCESS)
                    .build();

        } catch (RuntimeException e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(ExceptionConverter.convertToErrorCode(e))
                    .message(e.getMessage())
                    .build();
        }
    }

    private ResourceModel translateResponse(
            final DescribeProtectionResponse response,
            final AmazonWebServicesClientProxy proxy) {

        final Protection protection = response.protection();
        return ResourceModel.builder()
                .protectionId(protection.id())
                .name(protection.name())
                .protectionArn(protection.protectionArn())
                .resourceArn(protection.resourceArn())
                .tags(
                        HandlerHelper.getTags(
                                proxy,
                                this.client,
                                protection.resourceArn(),
                                tag ->
                                        Tag.builder()
                                                .key(tag.key())
                                                .value(tag.value())
                                                .build()))
                .healthCheckArns(
                        protection.healthCheckIds()
                                .stream()
                                .map(x -> HEALTH_CHECK_ARN_TEMPLATE + x)
                                .collect(Collectors.toList()))
                .applicationLayerAutomaticResponseConfiguration(
                        translateAppLayerAutoResponseConfig(protection))
                .build();
    }

    private static ApplicationLayerAutomaticResponseConfiguration translateAppLayerAutoResponseConfig(Protection protection) {

        String status = protection.applicationLayerAutomaticResponseConfiguration().statusAsString();
        Action action =
                protection.applicationLayerAutomaticResponseConfiguration().action().block() != null
                        ? Action.builder().block(Maps.newHashMap()).build()
                        : Action.builder().count(Maps.newHashMap()).build();


        return ApplicationLayerAutomaticResponseConfiguration.builder()
                .action(action)
                .status(status)
                .build();
    }
}
