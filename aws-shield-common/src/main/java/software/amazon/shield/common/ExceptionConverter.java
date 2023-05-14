package software.amazon.shield.common;

import java.util.Map;

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

public class ExceptionConverter<T> {

    private static final Map<Class<? extends ShieldException>, HandlerErrorCode> MAPPING =
        ImmutableMap.<Class<? extends ShieldException>, HandlerErrorCode>builder()
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
            .put(OptimisticLockException.class, HandlerErrorCode.NotStabilized)
            .put(ResourceAlreadyExistsException.class, HandlerErrorCode.AlreadyExists)
            .put(ResourceNotFoundException.class, HandlerErrorCode.NotFound)
            .build();

    public static HandlerErrorCode convertToErrorCode(RuntimeException error) {
        HandlerErrorCode translatedErrorCode = MAPPING.get(error.getClass());

        return translatedErrorCode == null
            ? HandlerErrorCode.GeneralServiceException
            : translatedErrorCode;
    }
}
