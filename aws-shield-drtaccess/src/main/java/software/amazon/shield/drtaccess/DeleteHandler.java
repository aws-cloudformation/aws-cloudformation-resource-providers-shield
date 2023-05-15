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
public class DeleteHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient shieldClient;

    public DeleteHandler() {
        this.shieldClient = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger
    ) {
        logger.log(String.format(
            "DeleteHandler: DRTAccess AccountID = %s, ClientToken = %s",
            request.getAwsAccountId(),
            request.getClientRequestToken()
        ));

        if (!HandlerHelper.accountIdMatchesResourcePrimaryId(request)) {
            logger.log("[Error] - Failed to handle delete request due to account ID not found.");
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
                if (!HandlerHelper.isDrtAccessConfigured(
                    progress.getCallbackContext().getRoleArn(),
                    progress.getCallbackContext().getLogBucketList()
                )) {
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.NotFound)
                        .message(HandlerHelper.NO_DRTACCESS_ERROR_MSG)
                        .build();
                }
                return HandlerHelper.disassociateDrtLogBucketList(
                    "DeleteHandler",
                    proxy,
                    proxy.newProxy(() -> shieldClient),
                    progress.getResourceModel(),
                    progress.getCallbackContext().getLogBucketList(),
                    progress.getCallbackContext(),
                    logger
                );
            })
            .then(progress -> HandlerHelper.disassociateDrtRole(
                "DeleteHandler",
                proxy,
                proxy.newProxy(() -> shieldClient),
                progress.getResourceModel(),
                progress.getCallbackContext(),
                logger
            ))
            .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }
}
