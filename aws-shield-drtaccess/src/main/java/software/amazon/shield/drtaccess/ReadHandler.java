package software.amazon.shield.drtaccess;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.drtaccess.helper.HandlerHelper;

@RequiredArgsConstructor
public class ReadHandler extends BaseHandler<CallbackContext> {
    private final ShieldClient shieldClient;

    public ReadHandler() {
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
            "ReadHandler: DRTAccess AccountID = %s, ClientToken = %s",
            request.getAwsAccountId(),
            request.getClientRequestToken()
        ));
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;

        if (!HandlerHelper.accountIdMatchesResourcePrimaryId(request)) {
            logger.log("[Error] - Failed to handle read request due to account ID not found.");
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.NotFound)
                .message(HandlerHelper.DRTACCESS_ACCOUNT_ID_NOT_FOUND_ERROR_MSG)
                .build();
        }

        return HandlerHelper.describeDrtAccessSetContext(
            "DeleteHandler",
            proxy,
            proxy.newProxy(() -> shieldClient),
            request.getDesiredResourceState(),
            callbackContext,
            logger
        ).then(progress -> {
            CallbackContext ctx = progress.getCallbackContext();
            if (!HandlerHelper.isDrtAccessConfigured(
                ctx.getRoleArn(),
                ctx.getLogBucketList()
            )) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NotFound)
                    .message(HandlerHelper.NO_DRTACCESS_ERROR_MSG)
                    .build();
            }
            final ResourceModel.ResourceModelBuilder resourceModelBuilder = ResourceModel.builder()
                .accountId(request.getAwsAccountId());

            if (ctx.getRoleArn() != null && !ctx.getRoleArn().isEmpty()) {
                resourceModelBuilder.roleArn(ctx.getRoleArn());
            }
            if (ctx.getLogBucketList() != null && !ctx.getLogBucketList().isEmpty()) {
                resourceModelBuilder.logBucketList(ctx.getLogBucketList());
            }
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(resourceModelBuilder.build())
                .status(OperationStatus.SUCCESS)
                .build();
        });
    }
}
