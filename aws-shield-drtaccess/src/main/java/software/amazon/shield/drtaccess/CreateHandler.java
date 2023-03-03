package software.amazon.shield.drtaccess;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessRequest;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.drtaccess.helper.HandlerHelper;

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

        try {
            if (!canCreateDrtAccess(proxy)) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.ResourceConflict)
                        .message(HandlerHelper.DRTACCESS_CONFLICT_ERROR_MSG)
                        .build();
            }
            HandlerHelper.associateDrtRole(proxy, client, model.getRoleArn());
            HandlerHelper.associateDrtLogBucketList(proxy, client, model.getLogBucketList());
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
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

    private boolean canCreateDrtAccess(AmazonWebServicesClientProxy proxy) {
        final DescribeDrtAccessRequest describeDrtAccessRequest = DescribeDrtAccessRequest.builder().build();
        final DescribeDrtAccessResponse describeDrtAccessResponse = proxy.injectCredentialsAndInvokeV2(
                describeDrtAccessRequest, client::describeDRTAccess);
        return describeDrtAccessResponse.roleArn() == null || describeDrtAccessResponse.roleArn().isEmpty();
    }
}
