package software.amazon.shield.protection;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.BlockAction;
import software.amazon.awssdk.services.shield.model.CountAction;
import software.amazon.awssdk.services.shield.model.DisableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.DisableApplicationLayerAutomaticResponseResponse;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.EnableApplicationLayerAutomaticResponseResponse;
import software.amazon.awssdk.services.shield.model.ResponseAction;
import software.amazon.awssdk.services.shield.model.UpdateApplicationLayerAutomaticResponseRequest;
import software.amazon.awssdk.services.shield.model.UpdateApplicationLayerAutomaticResponseResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.HandlerHelper;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;

import static software.amazon.shield.protection.helper.HandlerHelper.associateHealthChecks;
import static software.amazon.shield.protection.helper.HandlerHelper.disassociateHealthChecks;

@RequiredArgsConstructor
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient shieldClient;

    public UpdateHandler() {
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
                "UpdateHandler: ProtectionArn = %s, ClientToken = %s",
                request.getDesiredResourceState().getProtectionArn(),
                request.getClientRequestToken()
            )
        );
        logger.log(String.format(
                "UpdateHandler: ProtectionId = %s, ClientToken = %s",
                HandlerHelper.protectionArnToId(request.getDesiredResourceState().getProtectionArn()),
                request.getClientRequestToken()
            )
        );
        final ProxyClient<ShieldClient> proxyClient = proxy.newProxy(() -> this.shieldClient);
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;
        final ResourceModel currentState = request.getPreviousResourceState();
        final ResourceModel desiredState = request.getDesiredResourceState();

        return updateHealthCheckAssociation(
            desiredState.getHealthCheckArns(),
            currentState.getHealthCheckArns(),
            HandlerHelper.protectionArnToId(desiredState.getProtectionId()),
            proxy,
            proxyClient,
            desiredState,
            callbackContext,
            logger
        ).then(progress -> updateAppLayerAutoResponseConfig(
            desiredState.getApplicationLayerAutomaticResponseConfiguration(),
            currentState.getApplicationLayerAutomaticResponseConfiguration(),
            currentState.getResourceArn(),
            proxy,
            proxyClient,
            progress.getResourceModel(),
            progress.getCallbackContext(),
            logger
        )).then(progress -> HandlerHelper.updateTagsChainable(
            desiredState.getTags(),
            Tag::getKey,
            Tag::getValue,
            currentState.getTags(),
            Tag::getKey,
            Tag::getValue,
            progress.getResourceModel().getProtectionArn(),
            "Protection",
            "UpdateHandler",
            proxy,
            proxyClient,
            progress.getResourceModel(),
            progress.getCallbackContext(),
            logger
        )).then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateHealthCheckAssociation(
        @Nullable final List<String> desiredHealthCheckArns,
        @Nullable final List<String> currentHealthCheckArns,
        @NonNull final String protectionId,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {

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

        return disassociateHealthChecks(
            "UpdateHandler",
            protectionId,
            ImmutableList.copyOf(healthCheckArnsBefore),
            proxy,
            proxyClient,
            model,
            context,
            logger
        ).then(progress ->
            associateHealthChecks(
                "UpdateHandler",
                protectionId,
                ImmutableList.copyOf(healthCheckArnsAfter),
                proxy,
                proxyClient,
                model,
                context,
                logger
            )
        );
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateAppLayerAutoResponseConfig(
        @Nullable final ApplicationLayerAutomaticResponseConfiguration desiredConfig,
        @Nullable final ApplicationLayerAutomaticResponseConfiguration currentConfig,
        @NonNull final String resourceArn,
        @NonNull final AmazonWebServicesClientProxy proxy,
        @NonNull final ProxyClient<ShieldClient> proxyClient,
        @NonNull final ResourceModel model,
        @NonNull final CallbackContext context,
        @NonNull final Logger logger
    ) {

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
            return ProgressEvent.progress(model, context);
        }
        // case 1.2: remain enabled (however the action may have changed)
        else if (
            desiredStatus.equals("ENABLED") && currentStatus.equals("ENABLED")
        ) {
            if (desiredActionIsBlock == currentActionIsBlock) {
                return ProgressEvent.progress(model, context);
            }

            return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    UpdateApplicationLayerAutomaticResponseRequest,
                    UpdateApplicationLayerAutomaticResponseResponse>builder()
                .resourceType("Protection")
                .handlerName("UpdateHandler")
                .apiName("updateApplicationLayerAutomaticResponse")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(model)
                .context(context)
                .logger(logger)
                .translateToServiceRequest(m -> UpdateApplicationLayerAutomaticResponseRequest.builder()
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
                    .build()
                )
                .getRequestFunction(c -> c::updateApplicationLayerAutomaticResponse)
                .build()
                .initiate();
        }
        // case 2: state changed
        // case 2.1 enabled -> disabled
        else if (desiredStatus.equals("DISABLED") && currentStatus.equals("ENABLED")) {
            return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    DisableApplicationLayerAutomaticResponseRequest,
                    DisableApplicationLayerAutomaticResponseResponse>builder()
                .resourceType("Protection")
                .handlerName("UpdateHandler")
                .apiName("disableApplicationLayerAutomaticResponse")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(model)
                .context(context)
                .logger(logger)
                .translateToServiceRequest(m -> DisableApplicationLayerAutomaticResponseRequest.builder()
                    .resourceArn(resourceArn)
                    .build()
                )
                .getRequestFunction(c -> c::disableApplicationLayerAutomaticResponse)
                .build()
                .initiate();
        }
        // case 2.1 disabled -> enabled
        else if (desiredStatus.equals("ENABLED") && currentStatus.equals("DISABLED")) {
            return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    EnableApplicationLayerAutomaticResponseRequest,
                    EnableApplicationLayerAutomaticResponseResponse>builder()
                .resourceType("Protection")
                .handlerName("UpdateHandler")
                .apiName("enableApplicationLayerAutomaticResponse")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(model)
                .context(context)
                .logger(logger)
                .translateToServiceRequest(m -> EnableApplicationLayerAutomaticResponseRequest.builder()
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
                    .build()
                )
                .getRequestFunction(c -> c::enableApplicationLayerAutomaticResponse)
                .build()
                .initiate();
        }
        throw new RuntimeException("unreachable branch");
    }
}
