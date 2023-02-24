package software.amazon.shield.common;

import com.google.common.collect.ImmutableSet;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnClockSkewCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnExceptionsCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnStatusCodeCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnThrottlingCondition;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.io.IOException;
import java.time.Duration;

public class CustomerAPIClientBuilder {
    private static final BackoffStrategy BACKOFF_THROTTLING_STRATEGY =
            EqualJitterBackoffStrategy.builder()
                    .baseDelay(Duration.ofMillis(1500)) //account for jitter so 1st retry is ~1 sec
                    .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF)
                    .build();

    private static final RetryPolicy RETRY_POLICY =
            RetryPolicy.builder()
                    .numRetries(5) //average delay is ~30 sec if all retries attempted
                    .retryCondition(RetryCondition.defaultRetryCondition())
                    .throttlingBackoffStrategy(BACKOFF_THROTTLING_STRATEGY)
                    .build();

    private static final RetryPolicy PUT_LOGGING_CONFIG_RETRY_POLICY =
            RetryPolicy.builder()
                    .numRetries(5) //average delay is ~30 sec if all retries attempted
                    .retryCondition(OrRetryCondition.create(
                            RetryOnStatusCodeCondition.create(SdkDefaultRetrySetting.RETRYABLE_STATUS_CODES),
                            RetryOnExceptionsCondition
                                    .create(ImmutableSet.of(RetryableException.class,
                                            IOException.class)),
                            RetryOnClockSkewCondition.create(),
                            RetryOnThrottlingCondition.create()))
                    .throttlingBackoffStrategy(BACKOFF_THROTTLING_STRATEGY)
                    .build();

    public static ShieldClient getClient() {
        return ShieldClient.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RETRY_POLICY).build())
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
