package software.amazon.shield.proactiveengagement.helper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.DescribeEmergencyContactSettingsResponse;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.DisableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.EnableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.awssdk.services.shield.model.UpdateEmergencyContactSettingsRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.CallbackContext;
import software.amazon.shield.proactiveengagement.EmergencyContact;
import software.amazon.shield.proactiveengagement.ResourceModel;

public class HandlerHelper {

    public static final String SUBSCRIPTION_REQUIRED_ERROR_MSG = "Shield Advanced Subscription required.";
    public static final String ACCOUNT_ID_MISMATCH_ERROR_MSG = "Account ID mismatch.";
    public static final String NO_PROACTIVE_ENGAGEMENT_ERROR_MSG = "Proactive engagement is not configured on account.";
    public static final String PROACTIVE_ENGAGEMENT_ALREADY_CONFIGURED_ERROR_MSG = "Proactive engagement is already " +
        "configured on the account.";

    public static boolean callerAccountIdMatchesResourcePrimaryId(ResourceHandlerRequest<ResourceModel> request) {
        return request.getAwsAccountId() != null && request.getDesiredResourceState()
            .getAccountId()
            .equals(request.getAwsAccountId());
    }

    public static boolean isProactiveEngagementConfigured(
        DescribeSubscriptionResponse describeSubscriptionResponse,
        DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse
    ) {
        Subscription subscription = describeSubscriptionResponse.subscription();
        return subscription != null
            && subscription.proactiveEngagementStatus() != null
            && (
            subscription.proactiveEngagementStatus().equals(ProactiveEngagementStatus.ENABLED)
                || (
                describeEmergencyContactSettingsResponse.emergencyContactList() != null
                    && !describeEmergencyContactSettingsResponse.emergencyContactList().isEmpty()
            )
        );
    }

    public static ProgressEvent<ResourceModel, CallbackContext> disableProactiveEngagement(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            logger.log("Starting to disable proactive engagement.");
            return proxy.initiate("shield::disable-proactive-engagement", proxyClient, model, context)
                .translateToServiceRequest(m -> DisableProactiveEngagementRequest.builder().build())
                .makeServiceCall((req, client) -> proxy.injectCredentialsAndInvokeV2(req,
                    shieldClient::disableProactiveEngagement))
                .stabilize((r, response, client, m, c) -> HandlerHelper.stabilizeProactiveEngagementStatus(client))
                .handleError((request, e, client, m, callbackContext) -> {
                    logger.log("[Error] - Caught exception during disabling proactive engagement: " + e);
                    return ProgressEvent.failed(m,
                        callbackContext,
                        ExceptionConverter.convertToErrorCode((RuntimeException) e),
                        e.getMessage());
                })
                .done(res -> {
                    logger.log("Succeed disabling proactive engagement.");
                    return ProgressEvent.progress(model, context);
                });
        }
    }

    public static ProgressEvent<ResourceModel, CallbackContext> enableProactiveEngagement(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            logger.log("Starting to enable proactive engagement.");
            return proxy.initiate("shield::enable-proactive-engagement", proxyClient, model, context)
                .translateToServiceRequest(m -> EnableProactiveEngagementRequest.builder().build())
                .makeServiceCall((req, client) -> proxy.injectCredentialsAndInvokeV2(req,
                    shieldClient::enableProactiveEngagement))
                .stabilize((r, response, client, m, c) -> HandlerHelper.stabilizeProactiveEngagementStatus(client))
                .handleError((request, e, client, m, callbackContext) -> {
                    logger.log("[Error] - Caught exception during enabling proactive engagement: " + e);
                    return ProgressEvent.failed(
                        m,
                        callbackContext,
                        ExceptionConverter.convertToErrorCode((RuntimeException) e),
                        e.getMessage());
                })
                .done(res -> {
                    logger.log("Succeed enabling proactive engagement.");
                    return ProgressEvent.progress(model, context);
                });
        }
    }

    public static ProgressEvent<ResourceModel, CallbackContext> updateEmergencyContactSettings(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            logger.log("Starting to update proactive engagement settings.");
            return proxy.initiate("shield::update-emergency-contact-settings", proxyClient, model, context)
                .translateToServiceRequest(m -> UpdateEmergencyContactSettingsRequest.builder()
                    .emergencyContactList(HandlerHelper.convertCFNEmergencyContactList(model.getEmergencyContactList()))
                    .build())
                .makeServiceCall((req, client) -> proxy.injectCredentialsAndInvokeV2(req,
                    shieldClient::updateEmergencyContactSettings))
                .handleError((request, e, client, m, callbackContext) -> {
                    logger.log("[Error] - Caught exception during updating emergency contact settings: " + e);
                    return ProgressEvent.failed(
                        m,
                        callbackContext,
                        ExceptionConverter.convertToErrorCode((RuntimeException) e),
                        e.getMessage());
                })
                .done(res -> {
                    logger.log("Succeed updating proactive engagement settings.");
                    return ProgressEvent.progress(model, context);
                });
        }
    }

    public static boolean stabilizeProactiveEngagementStatus(final ProxyClient<ShieldClient> proxyClient) {
        DescribeSubscriptionRequest describeSubscriptionRequest = DescribeSubscriptionRequest.builder().build();
        DescribeEmergencyContactSettingsRequest.builder()
            .build();
        DescribeSubscriptionResponse describeSubscriptionResponse;
        try (ShieldClient shieldClient = proxyClient.client()) {
            describeSubscriptionResponse = proxyClient.injectCredentialsAndInvokeV2(describeSubscriptionRequest,
                shieldClient::describeSubscription);
        } catch (RuntimeException e) {
            return false;
        }
        return describeSubscriptionResponse.subscription() != null
            && describeSubscriptionResponse.subscription().proactiveEngagementStatus() != null
            && !describeSubscriptionResponse.subscription()
            .proactiveEngagementStatus()
            .equals(ProactiveEngagementStatus.PENDING);
    }

    public static List<EmergencyContact> convertSDKEmergencyContactList(List<software.amazon.awssdk.services.shield.model.EmergencyContact> emergencyContactList) {
        return Optional.ofNullable(emergencyContactList)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(con -> EmergencyContact.builder().phoneNumber(con.phoneNumber())
                .emailAddress(con.emailAddress())
                .contactNotes(con.contactNotes())
                .build())
            .collect(Collectors.toList());
    }

    public static List<software.amazon.awssdk.services.shield.model.EmergencyContact> convertCFNEmergencyContactList(
        List<EmergencyContact> emergencyContactList) {
        return Optional.ofNullable(emergencyContactList)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(con -> software.amazon.awssdk.services.shield.model.EmergencyContact.builder()
                .phoneNumber(con.getPhoneNumber())
                .emailAddress(con.getEmailAddress())
                .contactNotes(con.getContactNotes())
                .build())
            .collect(Collectors.toList());
    }
}
