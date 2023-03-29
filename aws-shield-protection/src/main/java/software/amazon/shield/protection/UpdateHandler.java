package software.amazon.shield.protection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateHealthCheckRequest;
import software.amazon.awssdk.services.shield.model.BlockAction;
import software.amazon.awssdk.services.shield.model.CountAction;
import software.amazon.awssdk.services.shield.model.DisableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.DisassociateHealthCheckRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.ResponseAction;
import software.amazon.awssdk.services.shield.model.TagResourceRequest;
import software.amazon.awssdk.services.shield.model.UntagResourceRequest;
import software.amazon.awssdk.services.shield.model.UpdateApplicationLayerAutomaticResponseRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.common.HandlerHelper;

@RequiredArgsConstructor
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public UpdateHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel currentState = request.getPreviousResourceState();
        final ResourceModel desiredState = request.getDesiredResourceState();
        final String protectionArn = desiredState.getProtectionArn();
        final String protectionId = HandlerHelper.protectionArnToId(protectionArn);
        logger.log(String.format("UpdateHandler: protection arn = %s", protectionArn));
        logger.log(String.format("UpdateHandler: protection id = %s", protectionId));

        try {
            updateHealthCheckAssociation(
                desiredState.getHealthCheckArns(),
                currentState.getHealthCheckArns(),
                protectionId,
                proxy
            );

            updateAppLayerAutoResponseConfig(
                desiredState.getApplicationLayerAutomaticResponseConfiguration(),
                currentState.getApplicationLayerAutomaticResponseConfiguration(),
                currentState.getResourceArn(),
                proxy
            );

            updateTags(
                desiredState.getTags(),
                currentState.getTags(),
                protectionArn,
                proxy
            );

            return readProtection(protectionArn, proxy, logger);

        } catch (RuntimeException e) {
            logger.log(String.format("UpdateHandler: error %s", e));
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(ExceptionConverter.convertToErrorCode(e))
                .message(e.getMessage())
                .build();
        }
    }

    private void updateTags(
        @Nullable final List<Tag> desiredTags,
        @Nullable final List<Tag> currentTags,
        @NonNull final String protectionArn,
        @NonNull final AmazonWebServicesClientProxy proxy) {

        final Map<String, String> currentTagsMap = Optional.ofNullable(currentTags)
            .orElse(Collections.emptyList())
            .stream()
            .collect(Collectors.toMap(
                Tag::getKey,
                Tag::getValue
            ));

        final List<software.amazon.awssdk.services.shield.model.Tag> tagsToSet = new ArrayList<>();

        Optional.ofNullable(desiredTags).orElse(Collections.emptyList()).forEach(tag -> {
            if (!(tag.getValue().equals(currentTagsMap.get(tag.getKey())))) {
                tagsToSet.add(software.amazon.awssdk.services.shield.model.Tag.builder()
                    .key(tag.getKey())
                    .value(tag.getValue())
                    .build());
            }
            currentTagsMap.remove(tag.getKey());
        });

        final List<String> tagsToRemove = new ArrayList<>(currentTagsMap.keySet());

        if (tagsToSet.size() > 0) {
            proxy.injectCredentialsAndInvokeV2(TagResourceRequest.builder()
                .tags(tagsToSet)
                .resourceARN(protectionArn)
                .build(), this.client::tagResource);
        }

        if (tagsToRemove.size() > 0) {
            proxy.injectCredentialsAndInvokeV2(UntagResourceRequest.builder()
                .tagKeys(tagsToRemove)
                .resourceARN(protectionArn)
                .build(), this.client::untagResource);
        }
    }

    private void updateHealthCheckAssociation(
        @Nullable final List<String> desiredHealthCheckArns,
        @Nullable final List<String> currentHealthCheckArns,
        @NonNull final String protectionId,
        @NonNull final AmazonWebServicesClientProxy proxy) {

        final Set<String> healthCheckArnsBefore = new HashSet<>(
            Optional.ofNullable(currentHealthCheckArns)
                .orElse(Collections.emptyList())
        );

        final Set<String> healthCheckArnsAfter = new HashSet<>(
            Optional.ofNullable(desiredHealthCheckArns)
                .orElse(Collections.emptyList())
        );

        final Set<String> intersection = new HashSet<>(healthCheckArnsBefore);
        intersection.retainAll(healthCheckArnsAfter);

        healthCheckArnsBefore.removeAll(intersection);
        healthCheckArnsAfter.removeAll(intersection);

        associateHealthChecks(protectionId, healthCheckArnsAfter, proxy);
        disassociateHealthChecks(protectionId, healthCheckArnsBefore, proxy);
    }

    private void associateHealthChecks(
        @NonNull final String protectionId,
        @NonNull final Set<String> healthCheckArns,
        @NonNull final AmazonWebServicesClientProxy proxy) {

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
        @NonNull final Set<String> healthCheckArns,
        @NonNull final AmazonWebServicesClientProxy proxy) {

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
        @Nullable final ApplicationLayerAutomaticResponseConfiguration desiredConfig,
        @Nullable final ApplicationLayerAutomaticResponseConfiguration currentConfig,
        @NonNull final String resourceArn,
        @NonNull final AmazonWebServicesClientProxy proxy) {

        final String desiredStatus = Optional.ofNullable(desiredConfig)
            .map(software.amazon.shield.protection.ApplicationLayerAutomaticResponseConfiguration::getStatus)
            .orElse("DISABLED");

        final String currentStatus = Optional.ofNullable(currentConfig)
            .map(software.amazon.shield.protection.ApplicationLayerAutomaticResponseConfiguration::getStatus)
            .orElse("DISABLED");

        final boolean desiredActionIsBlock = Optional.ofNullable(desiredConfig)
            .map(ApplicationLayerAutomaticResponseConfiguration::getAction)
            .map(Action::getBlock)
            .isPresent();

        final boolean currentActionIsBlock = Optional.ofNullable(currentConfig)
            .map(ApplicationLayerAutomaticResponseConfiguration::getAction)
            .map(Action::getBlock)
            .isPresent();

        // case 1: state unchanged
        // case 1.1: remain disabled
        if (desiredStatus.equals("DISABLED") && currentStatus.equals("DISABLED")) {
            return;
        }
        // case 1.2: remain enabled (however the action may have changed)
        else if (
            desiredStatus.equals("ENABLED") && currentStatus.equals("ENABLED")
        ) {
            if (desiredActionIsBlock == currentActionIsBlock) {
                return;
            }
            proxy.injectCredentialsAndInvokeV2(
                UpdateApplicationLayerAutomaticResponseRequest.builder()
                    .resourceArn(resourceArn)
                    .action(
                        desiredActionIsBlock
                            ?
                            ResponseAction.builder()
                                .block(BlockAction.builder().build())
                                .build()
                            : ResponseAction.builder()
                                .count(CountAction.builder().build())
                                .build()
                    )
                    .build(),
                this.client::updateApplicationLayerAutomaticResponse);
        }
        // case 2: state changed
        // case 2.1 enabled -> disabled
        else if (desiredStatus.equals("DISABLED") && currentStatus.equals("ENABLED")) {
            proxy.injectCredentialsAndInvokeV2(
                DisableApplicationLayerAutomaticResponseRequest.builder()
                    .resourceArn(resourceArn)
                    .build(),
                this.client::disableApplicationLayerAutomaticResponse);
        }
        // case 2.1 disabled -> enabled
        else if (desiredStatus.equals("ENABLED") && currentStatus.equals("DISABLED")) {
            proxy.injectCredentialsAndInvokeV2(
                EnableApplicationLayerAutomaticResponseRequest.builder()
                    .resourceArn(resourceArn)
                    .action(
                        desiredActionIsBlock
                            ?
                            ResponseAction.builder()
                                .block(BlockAction.builder().build())
                                .build()
                            : ResponseAction.builder()
                                .count(CountAction.builder().build())
                                .build()
                    )
                    .build(),
                this.client::enableApplicationLayerAutomaticResponse);
        }
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
