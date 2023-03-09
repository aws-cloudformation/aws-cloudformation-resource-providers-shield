package software.amazon.shield.subscription.helper;

import java.time.Instant;

import com.google.common.collect.Lists;
import software.amazon.awssdk.services.shield.model.DescribeSubscriptionResponse;
import software.amazon.awssdk.services.shield.model.ProactiveEngagementStatus;
import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.shield.common.Constants;
import software.amazon.shield.subscription.Limit;
import software.amazon.shield.subscription.ProtectionGroupArbitraryPatternLimits;
import software.amazon.shield.subscription.ProtectionGroupLimits;
import software.amazon.shield.subscription.ProtectionGroupPatternTypeLimits;
import software.amazon.shield.subscription.ProtectionLimits;
import software.amazon.shield.subscription.ResourceModel;
import software.amazon.shield.subscription.SubscriptionLimits;

public class SubscriptionTestData {
    public static final String NEXT_TOKEN = "TEST-next-token";

    public static final String SUBSCRIPTION_ARN = "TEST-subscription-arn";

    public static final String START_TIME = "2007-01-01T10:15:30Z";

    public static final String END_TIME = "2023-12-31T10:15:30Z";

    public static final int TIME_COMMITMENT_IN_SECONDS = 3600 * 24 * 365 * 10;

    public static final int PROTECTION_GROUP_MAX = 1234;

    public static final String LIMIT_TYPE = "TEST-limit-type";

    public static final int LIMIT_MAX = 123;

    public static final int MAX_MEMBER = 12;

    public static final ResourceModel RESOURCE_MODEL =
            ResourceModel.builder()
                    .subscriptionArn(SUBSCRIPTION_ARN)
                    .startTime(START_TIME)
                    .endTime(END_TIME)
                    .timeCommitmentInSeconds(TIME_COMMITMENT_IN_SECONDS)
                    .autoRenew(Constants.ENABLED)
                    .limits(
                            Lists.newArrayList(
                                    Limit.builder()
                                            .type(LIMIT_TYPE)
                                            .max(LIMIT_MAX)
                                            .build()))
                    .proactiveEngagementStatus(Constants.ENABLED)
                    .subscriptionLimits(
                            SubscriptionLimits.builder()
                                    .protectionGroupLimits(
                                            ProtectionGroupLimits.builder()
                                                    .maxProtectionGroups(PROTECTION_GROUP_MAX)
                                                    .patternTypeLimits(
                                                            ProtectionGroupPatternTypeLimits.builder()
                                                                    .arbitraryPatternLimits(
                                                                            ProtectionGroupArbitraryPatternLimits
                                                                                    .builder()
                                                                                    .maxMembers(MAX_MEMBER)
                                                                                    .build())
                                                                    .build())
                                                    .build())
                                    .protectionLimits(
                                            ProtectionLimits.builder()
                                                    .protectedResourceTypeLimits(
                                                            Lists.newArrayList(
                                                                    Limit.builder()
                                                                            .type(LIMIT_TYPE)
                                                                            .max(LIMIT_MAX)
                                                                            .build()))
                                                    .build())
                                    .build())
                    .build();

    public static final DescribeSubscriptionResponse DESCRIBE_SUBSCRIPTION_RESPONSE =
            DescribeSubscriptionResponse.builder()
                    .subscription(
                            Subscription.builder()
                                    .subscriptionArn(SubscriptionTestData.SUBSCRIPTION_ARN)
                                    .startTime(Instant.parse(SubscriptionTestData.START_TIME))
                                    .endTime(Instant.parse(SubscriptionTestData.END_TIME))
                                    .timeCommitmentInSeconds((long) SubscriptionTestData.TIME_COMMITMENT_IN_SECONDS)
                                    .autoRenew(Constants.ENABLED)
                                    .limits(
                                            Lists.newArrayList(
                                                    software.amazon.awssdk.services.shield.model.Limit.builder()
                                                            .type(SubscriptionTestData.LIMIT_TYPE)
                                                            .max((long) SubscriptionTestData.LIMIT_MAX)
                                                            .build()))
                                    .proactiveEngagementStatus(ProactiveEngagementStatus.ENABLED)
                                    .subscriptionLimits(
                                            software.amazon.awssdk.services.shield.model.SubscriptionLimits.builder()
                                                    .protectionGroupLimits(
                                                            software.amazon.awssdk.services.shield.model.ProtectionGroupLimits.builder()
                                                                    .maxProtectionGroups((long) SubscriptionTestData.PROTECTION_GROUP_MAX)
                                                                    .patternTypeLimits(
                                                                            software.amazon.awssdk.services.shield.model.ProtectionGroupPatternTypeLimits
                                                                                    .builder()
                                                                                    .arbitraryPatternLimits(
                                                                                            software.amazon.awssdk.services.shield.model.ProtectionGroupArbitraryPatternLimits.builder()
                                                                                                    .maxMembers((long) SubscriptionTestData.MAX_MEMBER)
                                                                                                    .build())
                                                                                    .build())
                                                                    .build())
                                                    .protectionLimits(
                                                            software.amazon.awssdk.services.shield.model.ProtectionLimits.builder()
                                                                    .protectedResourceTypeLimits(
                                                                            Lists.newArrayList(
                                                                                    software.amazon.awssdk.services.shield.model.Limit.builder()
                                                                                            .type(SubscriptionTestData.LIMIT_TYPE)
                                                                                            .max((long) SubscriptionTestData.LIMIT_MAX)
                                                                                            .build()))
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();
}
