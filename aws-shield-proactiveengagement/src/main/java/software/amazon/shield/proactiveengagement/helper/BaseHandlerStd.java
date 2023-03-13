package software.amazon.shield.proactiveengagement.helper;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.proactiveengagement.BaseHandler;
import software.amazon.shield.proactiveengagement.CallbackContext;
import software.amazon.shield.proactiveengagement.ResourceModel;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    public final EventualConsistencyHandlerHelper<ResourceModel, CallbackContext> eventualConsistencyHandlerHelper;
    public final ShieldClient shieldClient;

    public BaseHandlerStd() {
        this.eventualConsistencyHandlerHelper = new EventualConsistencyHandlerHelper<>();
        this.shieldClient = CustomerAPIClientBuilder.getClient();
    }

    public BaseHandlerStd(
            final ShieldClient shieldClient,
            final EventualConsistencyHandlerHelper<ResourceModel, CallbackContext> eventualConsistencyHandlerHelper) {
        this.shieldClient = shieldClient;
        this.eventualConsistencyHandlerHelper = eventualConsistencyHandlerHelper;
    }

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<ShieldClient> proxyClient,
            final Logger logger);

    protected abstract ProgressEvent<ResourceModel, CallbackContext> validateInput(
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final CallbackContext callbackContext,
            final ResourceHandlerRequest<ResourceModel> request
    );
}
