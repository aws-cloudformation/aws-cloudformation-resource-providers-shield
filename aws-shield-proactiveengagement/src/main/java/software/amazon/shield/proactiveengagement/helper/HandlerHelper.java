package software.amazon.shield.proactiveengagement.helper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.DisableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.DisableProactiveEngagementResponse;
import software.amazon.awssdk.services.shield.model.EnableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.EnableProactiveEngagementResponse;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.awssdk.services.shield.model.UpdateEmergencyContactSettingsRequest;
import software.amazon.awssdk.services.shield.model.UpdateEmergencyContactSettingsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;
import software.amazon.shield.proactiveengagement.CallbackContext;
import software.amazon.shield.proactiveengagement.ResourceModel;

public class HandlerHelper {

    public static final String SUBSCRIPTION_REQUIRED_ERROR_MSG = "Shield Advanced Subscription required.";
    public static final String ACCOUNT_ID_MISMATCH_ERROR_MSG = "Account ID mismatch.";
    public static final String NO_PROACTIVE_ENGAGEMENT_ERROR_MSG = "Proactive engagement is not configured on account.";
    public static final String PROACTIVE_ENGAGEMENT_ALREADY_CONFIGURED_ERROR_MSG = "Proactive engagement is already " +
        "configured on the account.";

    public static final String PROACTIVE_ENGAGEMENT_PENDING = "Proactive engagement is in pending status.";

    public static boolean callerAccountIdMatchesResourcePrimaryId(ResourceHandlerRequest<ResourceModel> request) {
        return request.getAwsAccountId() != null && request.getDesiredResourceState()
            .getAccountId()
            .equals(request.getAwsAccountId());
    }

    public static boolean isProactiveEngagementConfigured(
        Subscription subscription,
        List<software.amazon.awssdk.services.shield.model.EmergencyContact> emergencyContactList
    ) {
        return subscription != null
            && subscription.proactiveEngagementStatus() != null
            && (
            subscription.proactiveEngagementStatus().equals(ProactiveEngagementStatus.ENABLED)
                || (
                emergencyContactList != null
                    && !emergencyContactList.isEmpty()
            )
        );
    }

    public static ProgressEvent<ResourceModel, CallbackContext> disableProactiveEngagement(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DisableProactiveEngagementRequest,
                DisableProactiveEngagementResponse>builder()
            .resourceType("ProactiveEngagement")
            .handlerName(handlerName + ".HandlerHelper")
            .apiName("disableProactiveEngagement")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(context)
            .logger(logger)
            .translateToServiceRequest(
m-> DisableProactiveEngagementRequest.builder().build())
            .getRequestFunction(c -> c::disableProactiveEngagement)
            .build()
            .initiate()
            .then(progress -> stabilizeProactiveEngagementStatus(
                handlerName,
                proxy,
                proxyClient,
                progress.getResourceModel(),
                progress.getCallbackContext(),
                logger
            ));

    }

    public static ProgressEvent<ResourceModel, CallbackContext> enableProactiveEngagement(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, EnableProactiveEngagementRequest,
                EnableProactiveEngagementResponse>builder()
            .resourceType("ProactiveEngagement")
            .handlerName(handlerName + ".HandlerHelper")
            .apiName("enableProactiveEngagement")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(context)
            .logger(logger)
            .translateToServiceRequest(m -> EnableProactiveEngagementRequest.builder().build())
            .getRequestFunction(c -> c::enableProactiveEngagement)
            .build()
            .initiate()
            .then(progress -> stabilizeProactiveEngagementStatus(
                handlerName,
                proxy,
                proxyClient,
                progress.getResourceModel(),
                progress.getCallbackContext(),
                logger
            ));
    }

    public static ProgressEvent<ResourceModel, CallbackContext> updateEmergencyContactSettings(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, UpdateEmergencyContactSettingsRequest,
                UpdateEmergencyContactSettingsResponse>builder()
            .resourceType("ProactiveEngagement")
            .handlerName(handlerName + ".HandlerHelper")
            .apiName("updateEmergencyContactSettings")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(context)
            .logger(logger)
            .translateToServiceRequest(m -> UpdateEmergencyContactSettingsRequest.builder()
                .emergencyContactList(HandlerHelper.convertCFNEmergencyContactList(model.getEmergencyContactList()))
                .build())
            .getRequestFunction(c -> c::updateEmergencyContactSettings)
            .build()
            .initiate()
            .then(progress -> stabilizeProactiveEngagementStatus(
                handlerName,
                proxy,
                proxyClient,
                progress.getResourceModel(),
                progress.getCallbackContext(),
                logger
            ));
    }

    public static ProgressEvent<ResourceModel, CallbackContext> stabilizeProactiveEngagementStatus(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DescribeSubscriptionRequest,
                DescribeSubscriptionResponse>builder()
            .resourceType("ProactiveEngagement")
            .handlerName(handlerName + ".HandlerHelper")
            .apiName("stabilizeProactiveEngagementStatus(describeSubscription)")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(context)
            .logger(logger)
            .translateToServiceRequest(m -> DescribeSubscriptionRequest.builder().build())
            .getRequestFunction(c -> c::describeSubscription)
            .onSuccess((req, res, c, m, ctx) -> {
                if (res.subscription() != null
                    && res.subscription().proactiveEngagementStatus() != null
                    && !res.subscription().proactiveEngagementStatus().equals(ProactiveEngagementStatus.PENDING)
                ) {
                    return null;
                }
                return ProgressEvent.failed(
                    m,
                    ctx,
                    HandlerErrorCode.NotStabilized,
                    PROACTIVE_ENGAGEMENT_PENDING
                );
            })
            .build()
            .initiate();
    }

    public static List<software.amazon.shield.proactiveengagement.EmergencyContact> convertSDKEmergencyContactList(List<software.amazon.awssdk.services.shield.model.EmergencyContact> emergencyContactList) {
        return Optional.ofNullable(emergencyContactList)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(con -> software.amazon.shield.proactiveengagement.EmergencyContact.builder()
                .phoneNumber(con.phoneNumber())
                .emailAddress(con.emailAddress())
                .contactNotes(con.contactNotes())
                .build())
            .collect(Collectors.toList());
    }

    public static List<software.amazon.awssdk.services.shield.model.EmergencyContact> convertCFNEmergencyContactList(
        List<software.amazon.shield.proactiveengagement.EmergencyContact> emergencyContactList) {
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
