package software.amazon.shield.common;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
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

public class CustomerAPIClientBuilder {
    public static final Set<Class<? extends Exception>> CFN_RETRYABLE_EXCEPTIONS;

    static {
        Set<Class<? extends Exception>> retryableExceptions =
            new HashSet<>(SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS);
        retryableExceptions.add(OptimisticLockException.class);
        retryableExceptions.add(InternalErrorException.class);
        retryableExceptions.add(LimitsExceededException.class);
        CFN_RETRYABLE_EXCEPTIONS = ImmutableSet.copyOf(retryableExceptions);
    }

    private static final BackoffStrategy BACKOFF_THROTTLING_STRATEGY =
        EqualJitterBackoffStrategy.builder()
            .baseDelay(Duration.ofMillis(2000))
            .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF)
            .build();

    private static final RetryPolicy RETRY_POLICY =
        RetryPolicy.builder()
            .numRetries(5) //average delay is ~30 sec if all retries attempted
            .retryCondition(OrRetryCondition.create(
                    RetryOnStatusCodeCondition.create(SdkDefaultRetrySetting.RETRYABLE_STATUS_CODES),
                    RetryOnExceptionsCondition.create(CFN_RETRYABLE_EXCEPTIONS),
                    RetryOnClockSkewCondition.create(),
                    RetryOnThrottlingCondition.create(),
                    AndRetryCondition.create(
                        RetryOnExceptionsCondition.create(ImmutableSet.of(ShieldException.class)),
                        RetryOnErrorMessageSubStringCondition.create("Rate exceeded")
                    )
                )
            )
            .throttlingBackoffStrategy(BACKOFF_THROTTLING_STRATEGY)
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
