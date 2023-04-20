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
import software.amazon.awssdk.services.shield.model.AssociateDrtRoleRequest;
import software.amazon.awssdk.services.shield.model.AssociateDrtRoleResponse;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessResponse;
import software.amazon.awssdk.services.shield.model.DisassociateDrtRoleRequest;
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
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public UpdateHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        logger.log("Starting to handle update request.");
        final ResourceModel desiredState = request.getDesiredResourceState();
        final ResourceModel currentState = request.getPreviousResourceState();

        if (!HandlerHelper.accountIdMatchesResourcePrimaryId(request)) {
            logger.log("[Error] - Failed to handle update request due to account ID not found.");
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

        try {
            if (!HandlerHelper.isDrtAccessConfigured(
                desiredState.getRoleArn(),
                desiredState.getLogBucketList()
            )) {
                logger.log("[Error] - Failed to handle update request due to account not having DRT role associated.");
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NotFound)
                    .message(HandlerHelper.NO_DRTACCESS_ERROR_MSG)
                    .build();
            }

            // update logBucketList
            logger.log("Starting to update DRT log bucket list.");
            ImmutableList<String> oldLogBucketList =
                ImmutableList.copyOf(Optional.ofNullable(currentState.getLogBucketList())
                    .orElse(Collections.emptyList()));
            ImmutableList<String> newLogBucketList =
                ImmutableList.copyOf(Optional.ofNullable(desiredState.getLogBucketList())
                    .orElse(Collections.emptyList()));
            updateLogBucketList(proxy, oldLogBucketList, newLogBucketList, logger);
            logger.log("Succeed updating DRT log bucket list.");

            // update roleArn
            logger.log("Starting to update DRT access role.");
            updateDrtAccessRole(desiredState.getRoleArn(), currentState.getRoleArn(), proxy, logger);
            logger.log("Succeed updating DRT access role.");

            logger.log("Succeed handled update request.");
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(desiredState)
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

    private void updateLogBucketList(
        final AmazonWebServicesClientProxy proxy,
        @NonNull ImmutableList<String> oldList,
        @NonNull ImmutableList<String> newList,
        Logger logger) {
        List<String> removeList = new ArrayList<>(oldList);
        List<String> addList = new ArrayList<>(newList);
        removeList.removeAll(newList);
        addList.removeAll(oldList);

        // remove deleted list
        HandlerHelper.disassociateDrtLogBucketList(proxy, client, removeList, logger);
        // add new list
        HandlerHelper.associateDrtLogBucketList(proxy, client, addList, logger);
    }

    private void updateDrtAccessRole(
        @Nullable final String desiredRole,
        @Nullable final String currentRole,
        @NonNull final AmazonWebServicesClientProxy proxy,
        @NonNull final Logger logger
    ) {
        if (desiredRole != null && !desiredRole.isEmpty()) {
            // case 1. update associated role: the API replaces existing config, no need to call disassociate separately.
            // case 2. associate new role
            proxy.injectCredentialsAndInvokeV2(AssociateDrtRoleRequest.builder().roleArn(desiredRole).build(),
                client::associateDRTRole);
        } else {
            // case 3. disassociate existing role
            if (currentRole != null && !currentRole.isEmpty()) {
                proxy.injectCredentialsAndInvokeV2(DisassociateDrtRoleRequest.builder().build(),
                    client::disassociateDRTRole);
            }
        }
    }
}
