package software.amazon.shield.drtaccess;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.drtaccess.helper.HandlerHelper;

@RequiredArgsConstructor
public class ListHandler extends BaseHandler<CallbackContext> {
    private final ShieldClient client;

    public ListHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        try {
            final List<ResourceModel> models = new ArrayList<>();

            final DescribeDrtAccessResponse describeDrtAccessResponse =
                    HandlerHelper.getDrtAccessDescribeResponse(proxy, client, logger);
            if (HandlerHelper.noDrtAccess(describeDrtAccessResponse)) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(models)
                        .status(OperationStatus.SUCCESS)
                        .build();
            }

            models.add(ResourceModel.builder().accountId(request.getDesiredResourceState().getAccountId()).build());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(models)
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
}
