package software.amazon.shield.drtaccess;

import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
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
            DescribeDrtAccessResponse describeDrtAccessResponse = HandlerHelper.getDrtAccessDescribeResponse(proxy,
                client,
                logger);
            if (HandlerHelper.isDrtAccessConfigured(
                describeDrtAccessResponse.roleArn(),
                describeDrtAccessResponse.logBucketList()
            )) {
                logger.log(
                    "[Error] - Failed to handle create request due to account ID has been associated with DRT " +
                        "role or DRT log bucket list.");
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.ResourceConflict)
                    .message(HandlerHelper.DRTACCESS_CONFLICT_ERROR_MSG)
                    .build();
            }
            if (HandlerHelper.isEmptyDrtAccessRequest(model.getRoleArn(), model.getLogBucketList())) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.InvalidRequest)
                    .message(HandlerHelper.EMPTY_DRTACCESS_REQUEST)
                    .build();
            }
            HandlerHelper.associateDrtRole(proxy, client, model.getRoleArn(), logger);
            HandlerHelper.associateDrtLogBucketList(proxy, client, model.getLogBucketList(), logger);

            model.setAccountId(request.getAwsAccountId());
            return new ReadHandler(this.client).handleRequest(proxy, request, callbackContext, logger);
        } catch (RuntimeException e) {
            logger.log("[Error] - Caught exception during associating DRT role and DRT log bucket list: " + e);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(ExceptionConverter.convertToErrorCode(e))
                .message(e.getMessage())
                .build();
        }
    }
}
