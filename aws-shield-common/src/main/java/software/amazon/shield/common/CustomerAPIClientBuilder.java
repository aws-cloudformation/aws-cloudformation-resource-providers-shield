package software.amazon.shield.common;

import java.time.Duration;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.AndRetryCondition;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnClockSkewCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnExceptionsCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnStatusCodeCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnThrottlingCondition;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.InternalErrorException;
import software.amazon.awssdk.services.shield.model.LimitsExceededException;
import software.amazon.awssdk.services.shield.model.OptimisticLockException;
import software.amazon.awssdk.services.shield.model.ShieldException;
import software.amazon.cloudformation.LambdaWrapper;

import static java.time.temporal.ChronoUnit.SECONDS;

public class CustomerAPIClientBuilder {
    public static final Set<Class<? extends Exception>> CFN_RETRYABLE_EXCEPTIONS = ImmutableSet.of(
        OptimisticLockException.class,
        InternalErrorException.class,
        LimitsExceededException.class
    );

    private static final BackoffStrategy BACKOFF_STRATEGY =
        FixedDelayBackoffStrategy.create(Duration.of(2, SECONDS));

    private static final RetryPolicy RETRY_POLICY =
        RetryPolicy.builder()
            .numRetries(5) //average delay is ~30 sec if all retries attempted
            .retryCondition(OrRetryCondition.create(
                    RetryOnExceptionsCondition.create(SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS),
                    RetryOnStatusCodeCondition.create(SdkDefaultRetrySetting.RETRYABLE_STATUS_CODES),
                    RetryOnClockSkewCondition.create(),
                    RetryOnThrottlingCondition.create(),
                    RetryOnExceptionsCondition.create(CFN_RETRYABLE_EXCEPTIONS),
                    AndRetryCondition.create(
                        RetryOnExceptionsCondition.create(ImmutableSet.of(ShieldException.class)),
                        RetryOnErrorMessageSubStringCondition.create("Rate exceeded")
                    )
                )
            )
            .backoffStrategy(BACKOFF_STRATEGY)
            .throttlingBackoffStrategy(BACKOFF_STRATEGY)
            .build();

    public static ShieldClient getClient() {
        return ShieldClient.builder()
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .retryPolicy(RETRY_POLICY)
                .build())
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
    }
}
