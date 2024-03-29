package software.amazon.shield.common;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nullable;

import lombok.Builder;
import lombok.NonNull;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ShieldException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain.Callback;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Builder
public class ShieldAPIChainableRemoteCall<
    ResourceModelT,
    CallbackContextT extends StdCallbackContext,
    RequestT extends AwsRequest,
    ResponseT extends AwsResponse
    > {

    private static final String RATE_EXCEEDED_MSG = "rate exceeded";
    private static final int RATE_EXCEEDED_DELAY_SEC = 5;
    public static int JITTER_SECONDS = 2;

    public @NonNull
    final String resourceType;
    public @NonNull
    final String handlerName;
    public @NonNull
    final String apiName;

    public @NonNull
    final AmazonWebServicesClientProxy proxy;
    public @NonNull
    final ProxyClient<ShieldClient> proxyClient;
    public @NonNull
    final ResourceModelT model;
    public @NonNull
    final CallbackContextT context;
    public @NonNull
    final Logger logger;

    public @NonNull
    final Function<ResourceModelT, RequestT> translateToServiceRequest;
    public @NonNull
    final Function<ShieldClient, Function<RequestT, ResponseT>> getRequestFunction;

    /**
     * return ERROR with Throttling error code instead of IN_PROGRESS on rate exceeded.
     */
    public final boolean rateExceededIsCritical;

    /**
     * watch out when using stabilize.
     * when stabilize fails the entire call will be retried again, not just the stabilizer call
     * this can break idempotency for the handler
     * The recommended approach is to chain the stabilizer after an empty API call
     */
    public @Nullable
    final Stabilizer<ShieldClient, ResourceModelT, CallbackContextT, Boolean> stabilize;

    public @Nullable
    final Callback<RequestT, ResponseT, ShieldClient, ResourceModelT, CallbackContextT, ProgressEvent<ResourceModelT,
        CallbackContextT>> onSuccess;

    @FunctionalInterface
    public interface Stabilizer<ClientT, ModelT, CallbackT extends StdCallbackContext, ReturnT> {
        ReturnT invoke(
            ProxyClient<ClientT> client,
            ModelT model,
            CallbackT context
        );
    }

    private String getCallGraph() {
        return String.format("%s:%s:%s", this.resourceType, this.handlerName, this.apiName);
    }

    private ResponseT makeServiceCall(RequestT request, ProxyClient<ShieldClient> proxyClient) {
        // self throttling, to remedy low Shield API rate limits.
        if (JITTER_SECONDS > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(JITTER_SECONDS * 1000));
            } catch (InterruptedException ignored) {

            }
        }

        return proxy.injectCredentialsAndInvokeV2(request, getRequestFunction.apply(proxyClient.client()));
    }

    private Boolean isRateExceededException(Exception e) {
        return e instanceof ShieldException && e.getMessage().toLowerCase().contains(RATE_EXCEEDED_MSG);
    }

    private ProgressEvent<ResourceModelT, CallbackContextT> handleError(
        RequestT request,
        Exception e,
        ProxyClient<ShieldClient> client,
        ResourceModelT model,
        CallbackContextT context
    ) {
        final String callGraph = this.getCallGraph();
        if (isRateExceededException(e)) {
            logger.log(String.format("[WARN] Rate exceeded Requesting %s: %s", callGraph, e.toString()));
            // In theory returning throttling would be most ideal
            // however, CFN only retry errors to a certain extent
            // for Shield, long retries are essential due to low api rate throttle limits.
            // we return in_progress instead of throttling to get a third chance for the handler.
            //
            // However we still need to return throttling in certain cases. Namely for CreateHandler:
            // https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
            // > Every model MUST include the primaryIdentifier. The only exception is if the first progress event is
            // > FAILED, and the resource hasn’t yet been created.

            final ProgressEvent<ResourceModelT, CallbackContextT> progress;
            if (this.rateExceededIsCritical) {
                progress = ProgressEvent.failed(
                    model,
                    context,
                    HandlerErrorCode.Throttling,
                    e.getMessage()
                );
                progress.setCallbackDelaySeconds(RATE_EXCEEDED_DELAY_SEC);
            } else {
                progress = ProgressEvent.defaultInProgressHandler(
                    context,
                    RATE_EXCEEDED_DELAY_SEC,
                    model
                );
                progress.setMessage(e.getMessage());
            }
            return progress;
        }
        logger.log(String.format("[Error] Failed Requesting %s: %s", callGraph, e.toString()));
        return ProgressEvent.failed(
            model,
            context,
            ExceptionConverter.convertToErrorCode((RuntimeException) e),
            e.getMessage()
        );
    }

    private Boolean onStabilize(
        final RequestT request,
        final ResponseT response,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModelT resourceModel,
        final CallbackContextT callbackContext
    ) {
        final String callGraph = this.getCallGraph();
        logger.log(String.format("[INFO] Stabilizing Requesting %s", callGraph));
        if (this.stabilize != null) {
            try {
                return this.stabilize.invoke(
                    proxyClient,
                    resourceModel,
                    callbackContext
                );
            } catch (ShieldException e) {
                if (this.isRateExceededException(e)) {
                    logger.log(String.format("[WARN] Rate exceeded Stabilizing %s: %s", callGraph, e));
                    return false;
                }
                throw e;
            }
        }
        return true;
    }

    private ProgressEvent<ResourceModelT, CallbackContextT> onDone(
        final RequestT request,
        final ResponseT response,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModelT resourceModel,
        final CallbackContextT callbackContext
    ) {
        final String callGraph = this.getCallGraph();
        logger.log(String.format("[INFO] Completed Requesting %s", callGraph));
        if (this.onSuccess != null) {
            ProgressEvent<ResourceModelT, CallbackContextT> results = this.onSuccess.invoke(
                request,
                response,
                proxyClient,
                resourceModel,
                callbackContext
            );
            if (results != null) {
                return results;
            }
        }
        return ProgressEvent.defaultInProgressHandler(context, 0, model);
    }

    public ProgressEvent<ResourceModelT, CallbackContextT> initiate() {
        final String callGraph = this.getCallGraph();
        logger.log(String.format("[INFO] Start Requesting %s", callGraph));
        ProgressEvent<ResourceModelT, CallbackContextT> progress = this.proxy.initiate(
                callGraph,
                proxyClient,
                model,
                context
            )
            .translateToServiceRequest(this.translateToServiceRequest)
            .makeServiceCall(this::makeServiceCall)
            .handleError(this::handleError)
            .done(this::onDone);
        if (this.stabilize != null) {
            progress = progress.then(p ->
                this.proxy.initiate(
                        String.format("%s:%s", callGraph, "stabilize"),
                        proxyClient,
                        p.getResourceModel(),
                        p.getCallbackContext()
                    )
                    .translateToServiceRequest((ignored) -> (RequestT) null)
                    .makeServiceCall((ignored1, ignored2) -> (ResponseT) null)
                    .stabilize(this::onStabilize)
                    .handleError(this::handleError)
                    .progress()
            );
        }
        return progress;
    }
}
