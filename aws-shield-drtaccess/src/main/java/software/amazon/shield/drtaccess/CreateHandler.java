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
public class CreateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient shieldClient;

    public CreateHandler() {
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
                "CreateHandler: DRTAccess AccountID = %s, ClientToken = %s",
                request.getAwsAccountId(),
                request.getClientRequestToken()
            )
        );
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;

        return HandlerHelper.describeDrtAccessSetContext(
                "CreateHandler",
                proxy,
                proxy.newProxy(() -> shieldClient),
                request.getDesiredResourceState(),
                callbackContext,
                logger
            ).then(progress -> {
                if (HandlerHelper.isDrtAccessConfigured(
                    progress.getCallbackContext().getRoleArn(),
                    progress.getCallbackContext().getLogBucketList()
                )
                ) {
                    logger.log(
                        "[Error] CreateHandler early exit due to DRTAccess already configured.");
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.ResourceConflict)
                        .message(HandlerHelper.DRTACCESS_CONFLICT_ERROR_MSG)
                        .build();
                }
                if (HandlerHelper.isEmptyDrtAccessRequest(
                    progress.getResourceModel().getRoleArn(),
                    progress.getResourceModel().getLogBucketList()
                )) {
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.InvalidRequest)
                        .message(HandlerHelper.EMPTY_DRTACCESS_REQUEST)
                        .build();
                }
                return HandlerHelper.associateDrtRole(
                    "CreateHandler",
                    proxy,
                    proxy.newProxy(() -> shieldClient),
                    progress.getResourceModel(),
                    progress.getResourceModel().getRoleArn(),
                    progress.getCallbackContext(),
                    logger
                );
            })
            .then(progress -> HandlerHelper.associateDrtLogBucketList(
                "CreateHandler",
                proxy,
                proxy.newProxy(() -> shieldClient),
                progress.getResourceModel(),
                progress.getResourceModel().getLogBucketList(),
                progress.getCallbackContext(),
                logger
            ))
            .then(progress -> {
                ResourceModel model = progress.getResourceModel();
                model.setAccountId(request.getAwsAccountId());
                return ProgressEvent.defaultSuccessHandler(model);
            });
    }
}
