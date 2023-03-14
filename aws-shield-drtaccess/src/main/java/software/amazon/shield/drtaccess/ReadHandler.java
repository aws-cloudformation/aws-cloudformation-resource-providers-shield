package software.amazon.shield.drtaccess;

import software.amazon.awssdk.services.shield.ShieldClient;
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

public class ReadHandler extends BaseHandler<CallbackContext> {
    private final ShieldClient client;

    public ReadHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        if (!HandlerHelper.accountIdMatchesResourcePrimaryId(request)) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NotFound)
                    .message(HandlerHelper.DRTACCESS_ACCOUNT_ID_NOT_FOUND_ERROR_MSG)
                    .build();
        }

        try {
            final DescribeDrtAccessResponse describeDrtAccessResponse =
                    HandlerHelper.getDrtAccessDescribeResponse(proxy,
                            client);
            if (HandlerHelper.noDrtAccess(describeDrtAccessResponse)) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.NotFound)
                        .message(HandlerHelper.NO_DRTACCESS_ERROR_MSG)
                        .build();
            }
            final ResourceModel resourceModel = ResourceModel.builder()
                    .accountId(request.getAwsAccountId())
                    .roleArn(describeDrtAccessResponse.roleArn())
                    .build();

            if (describeDrtAccessResponse.hasLogBucketList()) {
                resourceModel.setLogBucketList(describeDrtAccessResponse.logBucketList());
            }
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(resourceModel)
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
