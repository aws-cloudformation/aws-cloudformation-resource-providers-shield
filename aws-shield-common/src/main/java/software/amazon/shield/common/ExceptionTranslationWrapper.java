package software.amazon.shield.common;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.shield.model.AccessDeniedException;
import software.amazon.awssdk.services.shield.model.AccessDeniedForDependencyException;
import software.amazon.awssdk.services.shield.model.InternalErrorException;
import software.amazon.awssdk.services.shield.model.InvalidOperationException;
import software.amazon.awssdk.services.shield.model.InvalidPaginationTokenException;
import software.amazon.awssdk.services.shield.model.InvalidParameterException;
import software.amazon.awssdk.services.shield.model.InvalidResourceException;
import software.amazon.awssdk.services.shield.model.LimitsExceededException;
import software.amazon.awssdk.services.shield.model.LockedSubscriptionException;
import software.amazon.awssdk.services.shield.model.NoAssociatedRoleException;
import software.amazon.awssdk.services.shield.model.OptimisticLockException;
import software.amazon.awssdk.services.shield.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.shield.model.ResourceNotFoundException;
import software.amazon.awssdk.services.shield.model.ShieldException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.Map;

public abstract class ExceptionTranslationWrapper<T> {

    private static final String MISSING_CRITICAL_INFO = "Critical information is missing in your request";
    private static final Map<Class<? extends ShieldException>, HandlerErrorCode> MAPPING;

    static {
        MAPPING = ImmutableMap.<Class<? extends ShieldException>, HandlerErrorCode>builder()
                .put(AccessDeniedException.class, HandlerErrorCode.AccessDenied)
                .put(AccessDeniedForDependencyException.class, HandlerErrorCode.AccessDenied)
                .put(InternalErrorException.class, HandlerErrorCode.ServiceInternalError)
                .put(InvalidOperationException.class, HandlerErrorCode.InvalidRequest)
                .put(InvalidPaginationTokenException.class, HandlerErrorCode.InvalidRequest)
                .put(InvalidParameterException.class, HandlerErrorCode.InvalidRequest)
                .put(InvalidResourceException.class, HandlerErrorCode.InvalidRequest)
                .put(LimitsExceededException.class, HandlerErrorCode.ServiceLimitExceeded)
                .put(LockedSubscriptionException.class, HandlerErrorCode.ResourceConflict)
                .put(NoAssociatedRoleException.class, HandlerErrorCode.InvalidCredentials)
                .put(OptimisticLockException.class, HandlerErrorCode.ResourceConflict)
                .put(ResourceAlreadyExistsException.class, HandlerErrorCode.AlreadyExists)
                .put(ResourceNotFoundException.class, HandlerErrorCode.NotFound)
                .build();
    }

    /**
     * Execute code with exception translation.
     *
     * @return T result of the function.
     * @throws RuntimeException WAFAPIException.
     */
    public final T execute() throws RuntimeException {
        try {
            return doWithTranslation();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Override with code to run and have exceptions translated.
     *
     * @return T result of the function.
     * @throws RuntimeException RuntimeException
     */
    public abstract T doWithTranslation() throws RuntimeException;

    /**
     * Translate exception to CloudFormation HandlerErrorCode.
     *
     * @param error error thrown by Customer API.
     * @return HandlerErrorCode code gets returned to customer.
     */
    public static HandlerErrorCode translateExceptionIntoErrorCode(RuntimeException error) {
        //based on CFN contract, we should throw HandlerErrorCode.NotFound if we are missing critical info
        if (error instanceof ShieldException
                && error.getMessage() != null
                && error.getMessage().startsWith(MISSING_CRITICAL_INFO)) {
            return HandlerErrorCode.NotFound;
        }

        HandlerErrorCode translatedErrorCode = MAPPING.get(error.getClass());

        return translatedErrorCode == null ? HandlerErrorCode.GeneralServiceException : translatedErrorCode;
    }
}
