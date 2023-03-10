package software.amazon.shield.subscription.helper;

import java.util.stream.Collectors;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionRequest;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.InvalidResourceException;
import software.amazon.awssdk.services.shield.model.ResourceNotFoundException;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.shield.common.Constants;
import software.amazon.shield.subscription.Limit;
import software.amazon.shield.subscription.ProtectionGroupArbitraryPatternLimits;
import software.amazon.shield.subscription.ProtectionGroupLimits;
import software.amazon.shield.subscription.ProtectionGroupPatternTypeLimits;
import software.amazon.shield.subscription.ProtectionLimits;
import software.amazon.shield.subscription.ResourceModel;
import software.amazon.shield.subscription.SubscriptionLimits;

public class ReadSubscriptionHelper {

    public static Subscription describeSubscription(
            final AmazonWebServicesClientProxy proxy,
            final ShieldClient client,
            final ResourceModel model) throws ResourceNotFoundException, InvalidResourceException {

        final DescribeSubscriptionResponse describeSubscriptionResponse =
                proxy.injectCredentialsAndInvokeV2(
                        DescribeSubscriptionRequest.builder().build(),
                        client::describeSubscription);

        final Subscription subscription = describeSubscriptionResponse.subscription();

        final String providedSubscriptionArn = model.getSubscriptionArn();
        final String returnedSubscriptionArn = subscription.subscriptionArn();

        if (!providedSubscriptionArn.equals(returnedSubscriptionArn)) {
            throw ResourceNotFoundException.builder()
                    .resourceType("Subscription")
                    .message("Subscription Not Found")
                    .build();
        }

        if (subscription.proactiveEngagementStatusAsString().equals(Constants.PENDING)) {
            throw InvalidResourceException.builder()
                    .message("ProactiveEngagementStatus is PENDING")
                    .build();
        }

        return subscription;
    }

    public static ResourceModel convertSubscription(final Subscription subscription) {
        return ResourceModel.builder()
                .subscriptionArn(subscription.subscriptionArn())
                .startTime(subscription.startTime().toString())
                .endTime(subscription.endTime().toString())
                .timeCommitmentInSeconds(Math.toIntExact(subscription.timeCommitmentInSeconds()))
                .autoRenew(subscription.autoRenewAsString())
                .limits(
                        subscription
                                .limits()
                                .stream()
                                .map(
                                        x ->
                                                Limit.builder()
                                                        .max(Math.toIntExact(x.max()))
                                                        .type(x.type())
                                                        .build())
                                .collect(Collectors.toList()))
                .proactiveEngagementStatus(subscription.proactiveEngagementStatusAsString())
                .subscriptionLimits(
                        SubscriptionLimits.builder()
                                .protectionLimits(
                                        convertProtectionLimits(subscription.subscriptionLimits().protectionLimits()))
                                .protectionGroupLimits(
                                        convertProtectionGroupLimits(
                                                subscription.subscriptionLimits().protectionGroupLimits()))
                                .build())
                .build();
    }

    public static ProtectionLimits convertProtectionLimits(
            final software.amazon.awssdk.services.shield.model.ProtectionLimits protectionLimits) {

        return ProtectionLimits.builder()
                .protectedResourceTypeLimits(
                        protectionLimits
                                .protectedResourceTypeLimits()
                                .stream()
                                .map(
                                        x ->
                                                Limit.builder()
                                                        .type(x.type())
                                                        .max(Math.toIntExact(x.max()))
                                                        .build())
                                .collect(Collectors.toList()))
                .build();
    }

    public static ProtectionGroupLimits convertProtectionGroupLimits(
            final software.amazon.awssdk.services.shield.model.ProtectionGroupLimits protectionGroupLimits) {

        return ProtectionGroupLimits.builder()
                .maxProtectionGroups(Math.toIntExact(protectionGroupLimits.maxProtectionGroups()))
                .patternTypeLimits(
                        ProtectionGroupPatternTypeLimits.builder()
                                .arbitraryPatternLimits(
                                        ProtectionGroupArbitraryPatternLimits.builder()
                                                .maxMembers(
                                                        Math.toIntExact(
                                                                protectionGroupLimits.patternTypeLimits()
                                                                        .arbitraryPatternLimits()
                                                                        .maxMembers()))
                                                .build())
                                .build())
                .build();
    }
}
