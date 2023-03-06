package software.amazon.shield.proactiveengagement;

import java.util.ArrayList;
import java.util.List;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final List<ResourceModel> models = new ArrayList<>();
        models.add(ResourceModel.builder().accountId(request.getDesiredResourceState().getAccountId()).build());

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
