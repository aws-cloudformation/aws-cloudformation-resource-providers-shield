package software.amazon.shield.drtaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
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
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient shieldClient;

    public UpdateHandler() {
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
            "UpdateHandler: DRTAccess AccountID = %s, ClientToken = %s",
            request.getAwsAccountId(),
            request.getClientRequestToken()
        ));
        callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;

        final ResourceModel desiredState = request.getDesiredResourceState();
        final ResourceModel currentState = request.getPreviousResourceState();

        if (!HandlerHelper.accountIdMatchesResourcePrimaryId(request)) {
            logger.log("[Error] UpdateHandler: Failed due to account ID not found.");
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.NotFound)
                .message(HandlerHelper.DRTACCESS_ACCOUNT_ID_NOT_FOUND_ERROR_MSG)
                .build();
        }
        if (HandlerHelper.isEmptyDrtAccessRequest(desiredState.getRoleArn(), desiredState.getLogBucketList())) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.InvalidRequest)
                .message(HandlerHelper.EMPTY_DRTACCESS_REQUEST)
                .build();
        }
        if (!HandlerHelper.isDrtAccessConfigured(
            desiredState.getRoleArn(),
            desiredState.getLogBucketList()
        )) {
            logger.log("[Error] UpdateHandler: Failed due to DRTAccess not configured.");
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.NotFound)
                .message(HandlerHelper.NO_DRTACCESS_ERROR_MSG)
                .build();
        }

        return ProgressEvent.progress(desiredState, callbackContext)
            .then(progress -> {
                // update logBucketList
                ImmutableList<String> oldLogBucketList =
                    ImmutableList.copyOf(Optional.ofNullable(currentState.getLogBucketList())
                        .orElse(Collections.emptyList()));
                ImmutableList<String> newLogBucketList =
                    ImmutableList.copyOf(Optional.ofNullable(desiredState.getLogBucketList())
                        .orElse(Collections.emptyList()));
                return updateLogBucketList(
                    proxy,
                    progress.getResourceModel(),
                    oldLogBucketList,
                    newLogBucketList,
                    progress.getCallbackContext(),
                    logger
                );
            })
            .then(progress -> {
                // update roleArn
                return updateDrtAccessRole(
                    proxy,
                    progress.getResourceModel(),
                    desiredState.getRoleArn(),
                    currentState.getRoleArn(),
                    progress.getCallbackContext(),
                    logger
                );

            })
            .then(progress -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(desiredState)
                .status(OperationStatus.SUCCESS)
                .build()
            );
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateLogBucketList(
        final AmazonWebServicesClientProxy proxy,
        final ResourceModel model,
        @NonNull ImmutableList<String> oldList,
        @NonNull ImmutableList<String> newList,
        final CallbackContext context,
        Logger logger
    ) {
        List<String> removeList = new ArrayList<>(oldList);
        List<String> addList = new ArrayList<>(newList);
        removeList.removeAll(newList);
        addList.removeAll(oldList);

        return HandlerHelper.disassociateDrtLogBucketList(
            "DeleteHandler",
            proxy,
            proxy.newProxy(() -> shieldClient),
            model,
            removeList,
            context,
            logger
        ).then(progress -> HandlerHelper.associateDrtLogBucketList(
            "DeleteHandler",
            proxy,
            proxy.newProxy(() -> shieldClient),
            progress.getResourceModel(),
            addList,
            progress.getCallbackContext(),
            logger
        ));
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateDrtAccessRole(
        final AmazonWebServicesClientProxy proxy,
        final ResourceModel model,
        @Nullable final String desiredRole,
        @Nullable final String currentRole,
        final CallbackContext context,
        @NonNull final Logger logger
    ) {
        ProgressEvent<ResourceModel, CallbackContext> ret = ProgressEvent.progress(model, context);
        if (desiredRole != null && !desiredRole.isEmpty()) {
            // case 1. update associated role: the API replaces existing config, no need to call disassociate
            // separately.
            // case 2. associate new role
            ret = ret.then(progress -> HandlerHelper.associateDrtRole(
                "DeleteHandler",
                proxy,
                proxy.newProxy(() -> shieldClient),
                progress.getResourceModel(),
                desiredRole,
                progress.getCallbackContext(),
                logger
            ));
        } else {
            // case 3. disassociate existing role
            if (currentRole != null && !currentRole.isEmpty()) {
                ret = ret.then(progress -> HandlerHelper.disassociateDrtRole(
                    "DeleteHandler",
                    proxy,
                    proxy.newProxy(() -> shieldClient),
                    progress.getResourceModel(),
                    progress.getCallbackContext(),
                    logger
                ));
            }
        }
        return ret;
    }
}
