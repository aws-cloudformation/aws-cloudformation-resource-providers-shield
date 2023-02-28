package software.amazon.shield.common;

import lombok.NonNull;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper containing common handler operations.
 */
public class HandlerHelper {

    /**
     * Returns the list of converted tags for a given resourceARN.
     *
     * @param <T>             the Tag type parameter
     * @param proxy           the AWSClient proxy, not null
     * @param client          the client, not null
     * @param resourceARN     the resource ARN, not null
     * @param convertFunction the convert function to convert from {@link Tag} to T
     * @return the list of tags for the resourceARN, will not be null
     */
    public static <T> List<T> getConvertedTags(@NonNull final AmazonWebServicesClientProxy proxy,
                                               @NonNull final ShieldClient client,
                                               @NonNull final String resourceARN,
                                               @NonNull final Function<Tag, T> convertFunction) {
        ListTagsForResourceRequest request = ListTagsForResourceRequest.builder()
                .resourceARN(resourceARN)
                .build();
        ListTagsForResourceResponse response = proxy.injectCredentialsAndInvokeV2(
                request, client::listTagsForResource);
        List<Tag> tags = response.tags();
        return tags.stream()
                .map(convertFunction)
                .collect(Collectors.toList());
    }
}
