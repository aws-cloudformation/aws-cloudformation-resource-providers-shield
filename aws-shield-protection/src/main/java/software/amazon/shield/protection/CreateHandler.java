package software.amazon.shield.protection;

import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateHealthCheckRequest;
import software.amazon.awssdk.services.shield.model.BlockAction;
import software.amazon.awssdk.services.shield.model.CountAction;
import software.amazon.awssdk.services.shield.model.CreateProtectionRequest;
import software.amazon.awssdk.services.shield.model.CreateProtectionRequest.Builder;
import software.amazon.awssdk.services.shield.model.CreateProtectionResponse;
import software.amazon.awssdk.services.shield.model.DeleteProtectionRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionResponse;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.ResponseAction;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;

@RequiredArgsConstructor
public class CreateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public CreateHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final String protectionId;

        try {
            final CreateProtectionRequest.Builder createProtectionRequest =
                CreateProtectionRequest.builder()
                    .name(model.getName())
                    .resourceArn(model.getResourceArn());

            populateTags(model, createProtectionRequest);

            final CreateProtectionResponse createProtectionResponse =
                proxy.injectCredentialsAndInvokeV2(createProtectionRequest.build(), this.client::createProtection);
            protectionId = createProtectionResponse.protectionId();
            logger.log(String.format("CreateHandler: new protection created id = %s", protectionId));
            model.setProtectionId(protectionId);
        } catch (RuntimeException e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(ExceptionConverter.convertToErrorCode(e))
                .message(e.getMessage())
                .build();
        }

        try {
            final DescribeProtectionRequest describeProtectionRequest =
                DescribeProtectionRequest.builder()
                    .protectionId(protectionId)
                    .build();

            final DescribeProtectionResponse describeProtectionResponse =
                proxy.injectCredentialsAndInvokeV2(describeProtectionRequest, this.client::describeProtection);

            final String protectionArn = describeProtectionResponse.protection().protectionArn();
            logger.log(String.format("CreateHandler: new protection created arn = %s", protectionArn));
            model.setProtectionArn(protectionArn);

            associateHealthChecks(model.getHealthCheckArns(), protectionId, proxy);
            enableApplicationLayerAutomaticResponse(
                model.getApplicationLayerAutomaticResponseConfiguration(),
                model.getResourceArn(),
                proxy);

            return ProgressEvent.defaultSuccessHandler(model);
        } catch (RuntimeException e) {
            proxy.injectCredentialsAndInvokeV2(
                DeleteProtectionRequest.builder().protectionId(protectionId).build(),
                this.client::deleteProtection
            );
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(ExceptionConverter.convertToErrorCode(e))
                .message(e.getMessage())
                .build();
        }
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

    private void associateHealthChecks(
        final List<String> healthCheckArns,
        @NonNull final String protectionId,
        @NonNull final AmazonWebServicesClientProxy proxy) {

        if (CollectionUtils.isNullOrEmpty(healthCheckArns)) {
            return;
        }

        healthCheckArns.forEach(
            arn -> {
                final AssociateHealthCheckRequest associateHealthCheckRequest =
                    AssociateHealthCheckRequest.builder()
                        .protectionId(protectionId)
                        .healthCheckArn(arn)
                        .build();

                proxy.injectCredentialsAndInvokeV2(
                    associateHealthCheckRequest,
                    this.client::associateHealthCheck);
            }
        );
    }

    private void enableApplicationLayerAutomaticResponse(
        final ApplicationLayerAutomaticResponseConfiguration appLayerAutoResponseConfig,
        @NonNull final String resourceArn,
        @NonNull final AmazonWebServicesClientProxy proxy) {

        if (appLayerAutoResponseConfig == null
            || appLayerAutoResponseConfig.getStatus().equals("DISABLED")) {
            return;
        }

        if (appLayerAutoResponseConfig.getAction().getBlock() != null) {
            enableAppLayerAutoResponseWithBlockAction(resourceArn, proxy);
        } else {
            enableAppLayerAutoResponseWithCountAction(resourceArn, proxy);
        }
    }

    private void enableAppLayerAutoResponseWithBlockAction(
        @NonNull final String resourceArn,
        @NonNull final AmazonWebServicesClientProxy proxy) {

        proxy.injectCredentialsAndInvokeV2(
            EnableApplicationLayerAutomaticResponseRequest.builder()
                .resourceArn(resourceArn)
                .action(
                    ResponseAction.builder()
                        .block(BlockAction.builder().build())
                        .build())
                .build(),
            this.client::enableApplicationLayerAutomaticResponse);
    }

    private void enableAppLayerAutoResponseWithCountAction(
        @NonNull final String resourceArn,
        @NonNull final AmazonWebServicesClientProxy proxy) {

        proxy.injectCredentialsAndInvokeV2(
            EnableApplicationLayerAutomaticResponseRequest.builder()
                .resourceArn(resourceArn)
                .action(
                    ResponseAction.builder()
                        .count(CountAction.builder().build())
                        .build())
                .build(),
            this.client::enableApplicationLayerAutomaticResponse);
    }
}
