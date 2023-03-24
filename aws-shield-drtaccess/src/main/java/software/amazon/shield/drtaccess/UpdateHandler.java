package software.amazon.shield.drtaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
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
        final ResourceModel model = request.getDesiredResourceState();

        if (!HandlerHelper.accountIdMatchesResourcePrimaryId(request)) {
            logger.log("[Error] - Failed to handle update request due to account ID not found.");
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NotFound)
                    .message(HandlerHelper.DRTACCESS_ACCOUNT_ID_NOT_FOUND_ERROR_MSG)
                    .build();
        }

        try {
            final DescribeDrtAccessResponse describeDrtAccessResponse =
                    HandlerHelper.getDrtAccessDescribeResponse(proxy, client, logger);
            if (HandlerHelper.noDrtAccess(describeDrtAccessResponse)) {
                logger.log("[Error] - Failed to handle update request due to account not having DRT role associated.");
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.NotFound)
                        .message(HandlerHelper.NO_DRTACCESS_ERROR_MSG)
                        .build();
            }

            ImmutableList<String> oldLogBucketList = ImmutableList.copyOf(describeDrtAccessResponse.logBucketList());
            ImmutableList<String> newLogBucketList = ImmutableList.copyOf(Optional.ofNullable(model.getLogBucketList())
                    .orElse(Collections.emptyList()));

            // update logBucketList
            logger.log("Starting to update DRT log bucket list.");
            updateLogBucketList(proxy, oldLogBucketList, newLogBucketList, logger);
            logger.log("Succeed updating DRT log bucket list.");
            // add new roleArn
            if (needToUpdateDrtRole(model.getRoleArn(), describeDrtAccessResponse)) {
                logger.log("Starting to update DRT access role.");
                HandlerHelper.associateDrtRole(proxy, client, model.getRoleArn(), logger);
                logger.log("Succeed updating DRT access role.");
            }
            logger.log("Succeed handling update request.");
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

    private void updateLogBucketList(
            final AmazonWebServicesClientProxy proxy,
            ImmutableList<String> oldList,
            ImmutableList<String> newList,
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

    private boolean needToUpdateDrtRole(String roleArn, DescribeDrtAccessResponse describeDrtAccessResponse) {
        return roleArn != null && !roleArn.isEmpty() && !roleArn.equalsIgnoreCase(describeDrtAccessResponse.roleArn());
    }
}
