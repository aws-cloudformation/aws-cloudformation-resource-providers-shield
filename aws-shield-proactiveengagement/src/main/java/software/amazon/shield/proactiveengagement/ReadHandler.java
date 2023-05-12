package software.amazon.shield.proactiveengagement;

import java.util.Collections;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.helper.BaseHandlerStd;
import software.amazon.shield.proactiveengagement.helper.HandlerHelper;

public class ReadHandler extends BaseHandlerStd {

    public ReadHandler() {
        super();
    }

    public ReadHandler(ShieldClient shieldClient) {
        super(shieldClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<ShieldClient> proxyClient,
        final Logger logger) {

        logger.log(String.format("ReadHandler: ProactiveEngagement AccountID = %s", request.getAwsAccountId()));
        if (!HandlerHelper.callerAccountIdMatchesResourcePrimaryId(request)) {
            return ProgressEvent.failed(request.getDesiredResourceState(),
                callbackContext,
                HandlerErrorCode.NotFound,
                HandlerHelper.ACCOUNT_ID_MISMATCH_ERROR_MSG);
        }

        final ResourceModel model = ResourceModel.builder().build();
        model.setAccountId(request.getAwsAccountId());
        model.setProactiveEngagementStatus(ProactiveEngagementStatus.DISABLED.toString());
        model.setEmergencyContactList(Collections.emptyList());

        try {
            final DescribeSubscriptionResponse describeSubscriptionResponse = proxy.injectCredentialsAndInvokeV2(
                DescribeSubscriptionRequest.builder().build(),
                shieldClient::describeSubscription);

            if (describeSubscriptionResponse.subscription() == null) {
                logger.log("ReadHandler: early exit due to no subscription.");
                return ProgressEvent.failed(
                    ResourceModel.builder().accountId(request.getAwsAccountId()).build(),
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    HandlerHelper.SUBSCRIPTION_REQUIRED_ERROR_MSG);
            }

            final DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse =
                proxy.injectCredentialsAndInvokeV2(
                    DescribeEmergencyContactSettingsRequest.builder().build(),
                    shieldClient::describeEmergencyContactSettings);

            if (!HandlerHelper.isProactiveEngagementConfigured(
                describeSubscriptionResponse,
                describeEmergencyContactSettingsResponse
            )) {
                logger.log("ReadHandler: early exit due to proactive engagement not configured.");
                return ProgressEvent.failed(
                    ResourceModel.builder().accountId(request.getAwsAccountId()).build(),
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    HandlerHelper.NO_PROACTIVE_ENGAGEMENT_ERROR_MSG);
            }

            if (ProactiveEngagementStatus.ENABLED.equals(describeSubscriptionResponse.subscription()
                .proactiveEngagementStatus())) {
                model.setProactiveEngagementStatus(ProactiveEngagementStatus.ENABLED.toString());
            }

            if (
                describeEmergencyContactSettingsResponse.emergencyContactList() != null
                    && !describeEmergencyContactSettingsResponse.emergencyContactList().isEmpty()
            ) {
                model.setEmergencyContactList(HandlerHelper.convertSDKEmergencyContactList(
                    describeEmergencyContactSettingsResponse.emergencyContactList()));
            }

            return ProgressEvent.defaultSuccessHandler(model);
        } catch (RuntimeException e) {
            logger.log("[ERROR] read ProactiveEngagement failed " + e);
            return ProgressEvent.failed(
                model,
                callbackContext,
                ExceptionConverter.convertToErrorCode(e),
                e.getMessage());
        }

    }
}
