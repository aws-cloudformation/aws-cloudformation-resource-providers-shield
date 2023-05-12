package software.amazon.shield.common;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

public class RetryOnErrorMessageSubStringCondition implements RetryCondition {
    private final String errorMessageSubString;

    public RetryOnErrorMessageSubStringCondition(final String errorMessageSubString) {
        this.errorMessageSubString = errorMessageSubString;
    }

    @Override
    public boolean shouldRetry(final RetryPolicyContext context) {
        SdkException exception = context.exception();
        if (exception == null) {
            return false;
        }
        return exception.getMessage().contains(this.errorMessageSubString);
    }

    public static RetryOnErrorMessageSubStringCondition create(String errorMessageSubString) {
        return new RetryOnErrorMessageSubStringCondition(errorMessageSubString);
    }

}
