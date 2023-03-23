package software.amazon.shield.common;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.NonNull;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.InvalidParameterException;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class HandlerHelper {
    private static final int MAX_LENGTH_RESOURCE_NAME = 50;

    public static <T> String generateName(@NonNull final ResourceHandlerRequest<T> request) {
        return IdentifierUtils.generateResourceIdentifier(
            request.getLogicalResourceIdentifier(),
            request.getClientRequestToken(),
            MAX_LENGTH_RESOURCE_NAME
        );
    }

    public static <T> List<T> getTags(
        @NonNull final AmazonWebServicesClientProxy proxy,
        @NonNull final ShieldClient client,
        @NonNull final String resourceArn,
        @NonNull final Function<Tag, T> converter) {

        ListTagsForResourceRequest request =
            ListTagsForResourceRequest.builder()
                .resourceARN(resourceArn)
                .build();

        return proxy
            .injectCredentialsAndInvokeV2(request, client::listTagsForResource)
            .tags()
            .stream()
            .map(converter)
            .collect(Collectors.toList());
    }

    public static String protectionArnToId(@NonNull final String protectionArn) {
        final int index = protectionArn.indexOf('/');
        return protectionArn.substring(index + 1);
    }
}
