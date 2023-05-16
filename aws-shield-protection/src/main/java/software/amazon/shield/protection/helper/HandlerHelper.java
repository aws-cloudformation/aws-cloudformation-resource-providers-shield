package software.amazon.shield.protection.helper;

import java.util.List;

import lombok.NonNull;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateHealthCheckRequest;
import software.amazon.awssdk.services.shield.model.AssociateHealthCheckResponse;
import software.amazon.awssdk.services.shield.model.DisassociateHealthCheckRequest;
import software.amazon.awssdk.services.shield.model.DisassociateHealthCheckResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;
import software.amazon.shield.protection.CallbackContext;
import software.amazon.shield.protection.ResourceModel;

public class HandlerHelper {
    public static ProgressEvent<ResourceModel, CallbackContext> associateHealthChecks(
        final String handlerName,
        @NonNull final String protectionId,
        final List<String> healthCheckArns,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        ProgressEvent<ResourceModel, CallbackContext> ret = ProgressEvent.defaultInProgressHandler(context, 0, model);
        if (CollectionUtils.isNullOrEmpty(healthCheckArns)) {
            return ret;
        }

        for (String arn : healthCheckArns) {
            ret = ret.then(progress -> ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    AssociateHealthCheckRequest,
                    AssociateHealthCheckResponse>builder()
                .resourceType("Protection")
                .handlerName(handlerName)
                .apiName("associateHealthCheck")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(progress.getResourceModel())
                .context(progress.getCallbackContext())
                .logger(logger)
                .translateToServiceRequest(m -> AssociateHealthCheckRequest.builder()
                    .protectionId(protectionId)
                    .healthCheckArn(arn)
                    .build())
                .getRequestFunction(c -> c::associateHealthCheck)
                .build()
                .initiate());
        }
        return ret;
    }

    public static ProgressEvent<ResourceModel, CallbackContext> disassociateHealthChecks(
        final String handlerName,
        @NonNull final String protectionId,
        final List<String> healthCheckArns,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        ProgressEvent<ResourceModel, CallbackContext> ret = ProgressEvent.defaultInProgressHandler(context, 0, model);
        if (CollectionUtils.isNullOrEmpty(healthCheckArns)) {
            return ret;
        }

        for (String arn : healthCheckArns) {
            ret = ret.then(progress -> ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext,
                    DisassociateHealthCheckRequest,
                    DisassociateHealthCheckResponse>builder()
                .resourceType("Protection")
                .handlerName(handlerName)
                .apiName("disassociateHealthCheck")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(progress.getResourceModel())
                .context(progress.getCallbackContext())
                .logger(logger)
                .translateToServiceRequest(m -> DisassociateHealthCheckRequest.builder()
                    .protectionId(protectionId)
                    .healthCheckArn(arn)
                    .build())
                .getRequestFunction(c -> c::disassociateHealthCheck)
                .build()
                .initiate());
        }
        return ret;
    }
}
