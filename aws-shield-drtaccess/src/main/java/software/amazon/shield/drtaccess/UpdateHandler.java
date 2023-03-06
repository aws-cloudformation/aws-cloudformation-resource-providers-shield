package software.amazon.shield.drtaccess;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessRequest;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.drtaccess.helper.HandlerHelper;

import java.util.List;

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

        try {
            final DescribeDrtAccessRequest describeDrtAccessRequest = DescribeDrtAccessRequest.builder().build();
            final DescribeDrtAccessResponse describeDrtAccessResponse = proxy.injectCredentialsAndInvokeV2(
                    describeDrtAccessRequest, client::describeDRTAccess);

            List<String> oldLogBucketList = describeDrtAccessResponse.logBucketList();

            // remove old roleArn and logBucketList
            disassociateDrtLogBucketList(model, proxy, client, oldLogBucketList);
            disassociateDrtRole(model, proxy, client);
            // add new roleArn and logBucketList
            associateDrtRole(model, proxy, client, model.getRoleArn());
            associateDrtLogBucketList(model, proxy, client, model.getLogBucketList());
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

    private void disassociateDrtRole(ResourceModel input, AmazonWebServicesClientProxy proxy, ShieldClient client) {
        if (input.getRoleArn() == null || input.getRoleArn().isEmpty()) return;
        HandlerHelper.disassociateDrtRole(proxy, client);
    }

    private void disassociateDrtLogBucketList(ResourceModel input, AmazonWebServicesClientProxy proxy, ShieldClient client, List<String> logBucketList) {
        if (input.getLogBucketList() == null || input.getLogBucketList().isEmpty()) return;
        HandlerHelper.disassociateDrtLogBucketList(proxy, client, logBucketList);
    }

    private void associateDrtRole(ResourceModel input, AmazonWebServicesClientProxy proxy, ShieldClient client, String roleArn) {
        if (input.getRoleArn() == null || input.getRoleArn().isEmpty()) return;
        HandlerHelper.associateDrtRole(proxy, client, roleArn);
    }

    private void associateDrtLogBucketList(ResourceModel input, AmazonWebServicesClientProxy proxy, ShieldClient client, List<String> logBucketList) {
        if (input.getLogBucketList() == null || input.getLogBucketList().isEmpty()) return;
        HandlerHelper.associateDrtLogBucketList(proxy, client, logBucketList);
    }
}
