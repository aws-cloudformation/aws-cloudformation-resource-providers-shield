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

        final ResourceModel model = request.getDesiredResourceState();

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

            ImmutableList<String> oldLogBucketList = ImmutableList.copyOf(describeDrtAccessResponse.logBucketList());
            ImmutableList<String> newLogBucketList = ImmutableList.copyOf(Optional.ofNullable(model.getLogBucketList())
                    .orElse(Collections.emptyList()));

            // update logBucketList
            updateLogBucketList(proxy, oldLogBucketList, newLogBucketList);
            // add new roleArn
            if (needToUpdateDrtRole(model.getRoleArn(), describeDrtAccessResponse)) {
                HandlerHelper.associateDrtRole(proxy, client, model.getRoleArn());
            }
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
            ImmutableList<String> newList) {
        List<String> removeList = new ArrayList<>(oldList);
        List<String> addList = new ArrayList<>(newList);
        removeList.removeAll(newList);
        addList.removeAll(oldList);

        // remove deleted list
        HandlerHelper.disassociateDrtLogBucketList(proxy, client, removeList);
        // add new list
        HandlerHelper.associateDrtLogBucketList(proxy, client, addList);
    }

    private boolean needToUpdateDrtRole(String roleArn, DescribeDrtAccessResponse describeDrtAccessResponse) {
        return roleArn != null && !roleArn.isEmpty() && !roleArn.equalsIgnoreCase(describeDrtAccessResponse.roleArn());
    }
}
