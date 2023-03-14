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
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.proactiveengagement.CallbackContext;
import software.amazon.shield.proactiveengagement.EmergencyContact;
import software.amazon.shield.proactiveengagement.ResourceModel;

public class HandlerHelper {

    public static final String PROACTIVE_ENGAGEMENT_ACCOUNT_ID_NOT_FOUND_ERROR_MSG = "Your account ID is not found.";
    public static final String NO_PROACTIVE_ENGAGEMENT_ERROR_MSG = "Your account is not proactive engaged.";
    public static final String PROACTIVE_ENGAGEMENT_CONFLICT_ERROR_MSG = "Your account is already proactive engaged.";

    public static boolean callerAccountIdMatchesResourcePrimaryId(ResourceHandlerRequest<ResourceModel> request) {
        return request.getAwsAccountId() != null && request.getDesiredResourceState()
                .getPrimaryIdentifier()
                .getString("/properties/AccountId")
                .equals(request.getAwsAccountId());
    }

    public static boolean doesProactiveEngagementStatusExist(DescribeSubscriptionResponse describeSubscriptionResponse) {
        Subscription subscription = describeSubscriptionResponse.subscription();
        return subscription != null && subscription.proactiveEngagementStatus() != null;
    }

    public static boolean isProactiveEngagementEnabled(
            DescribeEmergencyContactSettingsResponse describeEmergencyContactSettingsResponse,
            DescribeSubscriptionResponse describeSubscriptionResponse) {
        Subscription subscription = describeSubscriptionResponse.subscription();
        return subscription.proactiveEngagementStatus() != null && subscription.proactiveEngagementStatus()
                .equals(ProactiveEngagementStatus.ENABLED) && describeEmergencyContactSettingsResponse.hasEmergencyContactList();
    }

    public static ProgressEvent<ResourceModel, CallbackContext> describeEmergencyContactSettings(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final boolean isReadHandler
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
                                    HandlerHelper.NO_PROACTIVE_ENGAGEMENT_ERROR_MSG);
                        }
                        if (isReadHandler) {
                            model.setEmergencyContactList(HandlerHelper.convertSDKEmergencyContactList(r.emergencyContactList()));
                        }
                        return ProgressEvent.progress(model, context);
                    });
        }
    }

    public static ProgressEvent<ResourceModel, CallbackContext> describeSubscription(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            return proxy.initiate("shield::describe-subscription", proxyClient, model, context)
                    .translateToServiceRequest(m -> DescribeSubscriptionRequest.builder().build())
                    .makeServiceCall((request, client) -> proxy.injectCredentialsAndInvokeV2(request,
                            shieldClient::describeSubscription))
                    .handleError((request, e, client, m, callbackContext) -> ProgressEvent.failed(m,
                            callbackContext,
                            ExceptionConverter.convertToErrorCode((RuntimeException) e),
                            e.getMessage()))
                    .done(res -> {
                        if (!HandlerHelper.doesProactiveEngagementStatusExist(res)) {
                            return ProgressEvent.failed(model,
                                    context,
                                    HandlerErrorCode.NotFound,
                                    HandlerHelper.NO_PROACTIVE_ENGAGEMENT_ERROR_MSG);
                        }
                        model.setProactiveEngagementStatus(res.subscription().proactiveEngagementStatusAsString());
                        return ProgressEvent.progress(model, context);
                    });
        }
    }

    public static ProgressEvent<ResourceModel, CallbackContext> disableProactiveEngagement(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            return proxy.initiate("shield::disable-proactive-engagement", proxyClient, model, context)
                    .translateToServiceRequest(m -> DisableProactiveEngagementRequest.builder().build())
                    .makeServiceCall((req, client) -> proxy.injectCredentialsAndInvokeV2(req,
                            shieldClient::disableProactiveEngagement))
                    .handleError((request, e, client, m, callbackContext) -> ProgressEvent.failed(m,
                            callbackContext,
                            ExceptionConverter.convertToErrorCode((RuntimeException) e),
                            e.getMessage()))
                    .done(res -> ProgressEvent.progress(model, context));
        }
    }

    public static ProgressEvent<ResourceModel, CallbackContext> enableProactiveEngagement(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            return proxy.initiate("shield::enable-proactive-engagement", proxyClient, model, context)
                    .translateToServiceRequest(m -> EnableProactiveEngagementRequest.builder().build())
                    .makeServiceCall((req, client) -> proxy.injectCredentialsAndInvokeV2(req,
                            shieldClient::enableProactiveEngagement))
                    .handleError((request, e, client, m, callbackContext) -> ProgressEvent.failed(m,
                            callbackContext,
                            ExceptionConverter.convertToErrorCode((RuntimeException) e),
                            e.getMessage()))
                    .done(res -> ProgressEvent.progress(model, context));
        }
    }

    public static ProgressEvent<ResourceModel, CallbackContext> updateEmergencyContactSettings(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<ShieldClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context
    ) {
        try (ShieldClient shieldClient = proxyClient.client()) {
            return proxy.initiate("shield::update-emergency-contact-settings", proxyClient, model, context)
                    .translateToServiceRequest(m -> UpdateEmergencyContactSettingsRequest.builder()
                            .emergencyContactList(HandlerHelper.convertCFNEmergencyContactList(model.getEmergencyContactList()))
                            .build())
                    .makeServiceCall((req, client) -> proxy.injectCredentialsAndInvokeV2(req,
                            shieldClient::updateEmergencyContactSettings))
                    .handleError((request, e, client, m, callbackContext) -> ProgressEvent.failed(m,
                            callbackContext,
                            ExceptionConverter.convertToErrorCode((RuntimeException) e),
                            e.getMessage()))
                    .done(res -> ProgressEvent.progress(model, context));
        }
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

    public static ResourceModel copyNewModel(ResourceModel model) {
        return ResourceModel.builder()
                .accountId(model.getAccountId())
                .proactiveEngagementStatus(model.getProactiveEngagementStatus())
                .emergencyContactList(model.getEmergencyContactList())
                .build();

    }
}
