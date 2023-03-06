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

        try {
            final DescribeDrtAccessRequest describeDrtAccessRequest = DescribeDrtAccessRequest.builder().build();
            final DescribeDrtAccessResponse describeDrtAccessResponse = proxy.injectCredentialsAndInvokeV2(
                    describeDrtAccessRequest, client::describeDRTAccess);

            final ResourceModel resourceModel = ResourceModel.builder()
                    .accountId(request.getDesiredResourceState().getAccountId())
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
