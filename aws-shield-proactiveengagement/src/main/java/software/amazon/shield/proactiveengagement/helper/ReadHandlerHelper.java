package software.amazon.shield.proactiveengagement.helper;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.CallbackContext;
import software.amazon.shield.proactiveengagement.ResourceModel;

public class ReadHandlerHelper extends HandlerHelper {
    public static ProgressEvent<ResourceModel, CallbackContext> describeEmergencyContactSettings(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            return proxy.initiate("shield::describe-emergency-contact", proxyClient, model, context)
                    .translateToServiceRequest((m) -> DescribeEmergencyContactSettingsRequest.builder().build())
                    .makeServiceCall((request, client) -> proxy.injectCredentialsAndInvokeV2(request,
                            shieldClient::describeEmergencyContactSettings))
                    .handleError((request, e, client, m, callbackContext) -> ProgressEvent.failed(m,
                            callbackContext,
                            ExceptionConverter.convertToErrorCode((RuntimeException) e),
                            e.getMessage()))
                    .done((r) -> {
                        if (!r.hasEmergencyContactList() && model.getProactiveEngagementStatus()
                                .equals(ProactiveEngagementStatus.DISABLED.toString())) {
                            return ProgressEvent.failed(model,
                                    context,
                                    HandlerErrorCode.NotFound,
                                    ReadHandlerHelper.NO_PROACTIVE_ENGAGEMENT_ERROR_MSG);
                        }
                        model.setEmergencyContactList(ReadHandlerHelper.convertSDKEmergencyContactList(r.emergencyContactList()));
                        return ProgressEvent.progress(model, context);
                    });
        }
    }
}
