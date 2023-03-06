package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

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

        final ResourceModel model = request.getDesiredResourceState();

        try {

            DescribeEmergencyContactSettingsRequest describeEmergencyContactSettingsRequest =
                    DescribeEmergencyContactSettingsRequest.builder().build();
            DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
                    proxy.injectCredentialsAndInvokeV2(
                            describeEmergencyContactSettingsRequest,
                            client::describeEmergencyContactSettings);

            DescribeSubscriptionRequest describeSubscriptionRequest = DescribeSubscriptionRequest.builder().build();
            DescribeSubscriptionResponse describeSubscriptionResponse = proxy.injectCredentialsAndInvokeV2(
                    describeSubscriptionRequest,
                    client::describeSubscription);

            final ResourceModel resultModel = ResourceModel.builder()
                    .accountId(model.getAccountId())
                    .proactiveEngagementStatus(describeSubscriptionResponse.subscription()
                            .proactiveEngagementStatusAsString())
                    .emergencyContacts(HandlerHelper.convertSDKEmergencyContactList(
                            describeEmergencyContactSettingsResponse.emergencyContactList()))
                    .accountId(request.getDesiredResourceState().getAccountId())
                    .build();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(resultModel)
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
