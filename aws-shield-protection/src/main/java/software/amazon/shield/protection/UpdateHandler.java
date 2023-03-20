package software.amazon.shield.protection;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateHealthCheckRequest;
import software.amazon.awssdk.services.shield.model.BlockAction;
import software.amazon.awssdk.services.shield.model.CountAction;
import software.amazon.awssdk.services.shield.model.DescribeProtectionRequest;
import software.amazon.awssdk.services.shield.model.DisableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.DisassociateHealthCheckRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.Protection;
import software.amazon.awssdk.services.shield.model.ResponseAction;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;

@RequiredArgsConstructor
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    private static final String healthCheckArnTemplate = "arn:aws:route53:::healthcheck/";

    public UpdateHandler() {
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

        try {
            final Protection protection = getProtection(model, proxy);

            // 1. associate/disassociate healthChecks
            updateHealthCheckAssociation(model, protection, proxy);

            // 2. appLayerAutoResponseConfig
            updateAppLayerAutoResponseConfig(
                    model.getApplicationLayerAutomaticResponseConfiguration(),
                    model.getResourceArn(),
                    proxy);

            return readProtection(protectionArn, proxy, logger);

        } catch (RuntimeException e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(ExceptionConverter.convertToErrorCode(e))
                    .message(e.getMessage())
                    .build();
        }
    }

    private Protection getProtection(final ResourceModel model, final AmazonWebServicesClientProxy proxy) {

        final DescribeProtectionRequest describeProtectionRequest =
                DescribeProtectionRequest.builder()
                        .protectionId(model.getProtectionId())
                        .resourceArn(model.getResourceArn())
                        .build();

        return proxy
                .injectCredentialsAndInvokeV2(describeProtectionRequest, this.client::describeProtection)
                .protection();
    }

    private void updateHealthCheckAssociation(
            @NonNull final ResourceModel model,
            @NonNull final Protection protection,
            @NonNull final AmazonWebServicesClientProxy proxy) {

        final Set<String> healthCheckArnsBefore =
                protection.healthCheckIds()
                        .stream()
                        .map(x -> healthCheckArnTemplate + x)
                        .collect(Collectors.toSet());

        final Set<String> healthCheckArnsAfter = new HashSet<>(model.getHealthCheckArns());

        final Set<String> intersection =
                healthCheckArnsBefore
                        .stream()
                        .filter(healthCheckArnsAfter::contains)
                        .collect(Collectors.toSet());

        healthCheckArnsBefore.removeAll(intersection);
        healthCheckArnsAfter.removeAll(intersection);

        associateHealthChecks(protection.id(), healthCheckArnsAfter, proxy);
        disassociateHealthChecks(protection.id(), healthCheckArnsBefore, proxy);
    }

    private void associateHealthChecks(
            @NonNull final String protectionId,
            final Set<String> healthCheckArns,
            final AmazonWebServicesClientProxy proxy) {

        healthCheckArns.forEach(
                arn -> {
                    final AssociateHealthCheckRequest request =
                            AssociateHealthCheckRequest.builder()
                                    .protectionId(protectionId)
                                    .healthCheckArn(arn)
                                    .build();

                    proxy.injectCredentialsAndInvokeV2(
                            request,
                            this.client::associateHealthCheck);
                }
        );
    }

    private void disassociateHealthChecks(
            @NonNull final String protectionId,
            final Set<String> healthCheckArns,
            final AmazonWebServicesClientProxy proxy) {

        healthCheckArns.forEach(
                arn -> {
                    final DisassociateHealthCheckRequest request =
                            DisassociateHealthCheckRequest.builder()
                                    .protectionId(protectionId)
                                    .healthCheckArn(arn)
                                    .build();

                    proxy.injectCredentialsAndInvokeV2(
                            request,
                            this.client::disassociateHealthCheck);
                }
        );
    }

    private void updateAppLayerAutoResponseConfig(
            final ApplicationLayerAutomaticResponseConfiguration config,
            @NonNull final String resourceArn,
            @NonNull final AmazonWebServicesClientProxy proxy) {

        if (config.getStatus().equals("ENABLED")) {
            if (config.getAction().getBlock() != null) {
                enableAppLayerAutoResponseWithBlockAction(resourceArn, proxy);
            } else {
                enableAppLayerAutoResponseWithCountAction(resourceArn, proxy);
            }
        } else {
            disableAppLayerAutoResponse(resourceArn, proxy);
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

    private void disableAppLayerAutoResponse(
            @NonNull final String resourceArn,
            @NonNull final AmazonWebServicesClientProxy proxy) {

        proxy.injectCredentialsAndInvokeV2(
                DisableApplicationLayerAutomaticResponseRequest.builder()
                        .resourceArn(resourceArn)
                        .build(),
                this.client::disableApplicationLayerAutomaticResponse);
    }

    private ProgressEvent<ResourceModel, CallbackContext> readProtection(
            @NonNull final String protectionArn,
            @NonNull final AmazonWebServicesClientProxy proxy,
            @NonNull final Logger logger) {

        final ResourceModel readResourceModel =
                ResourceModel.builder()
                        .protectionArn(protectionArn)
                        .build();

        return new ReadHandler(this.client).handleRequest(
                proxy,
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(readResourceModel)
                        .build(),
                null,
                logger);
    }
}
