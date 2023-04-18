package software.amazon.shield.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.NonNull;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.awssdk.services.shield.model.TagResourceRequest;
import software.amazon.awssdk.services.shield.model.UntagResourceRequest;
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

    public static <T, S> void updateTags(
        @Nullable final List<? extends T> desiredTags,
        Function<? super T, String> desiredTagKeyGetter,
        Function<? super T, String> desiredTagValueGetter,
        @Nullable final List<? extends S> currentTags,
        Function<? super S, String> currentTagKeyGetter,
        Function<? super S, String> currentTagValueGetter,
        @NonNull final String resourceArn,
        @NonNull final ShieldClient shieldClient,
        @NonNull final AmazonWebServicesClientProxy proxy
    ) {

        final Map<String, String> currentTagsMap = Optional.ofNullable(currentTags)
            .orElse(Collections.emptyList())
            .stream()
            .collect(Collectors.toMap(
                currentTagKeyGetter,
                currentTagValueGetter
            ));

        final List<software.amazon.awssdk.services.shield.model.Tag> tagsToSet = new ArrayList<>();

        Optional.ofNullable(desiredTags).orElse(Collections.emptyList()).forEach(tag -> {
            final String desiredKey = desiredTagKeyGetter.apply(tag);
            final String currentValueAtDesiredKey = currentTagsMap.get(desiredKey);
            final String desiredValue = desiredTagValueGetter.apply(tag);
            if (!(desiredValue.equals(currentValueAtDesiredKey))) {
                tagsToSet.add(software.amazon.awssdk.services.shield.model.Tag.builder()
                    .key(desiredKey)
                    .value(desiredValue)
                    .build());
            }
            currentTagsMap.remove(desiredKey);
        });

        final List<String> tagsToRemove = new ArrayList<>(currentTagsMap.keySet());

        if (tagsToSet.size() > 0) {
            proxy.injectCredentialsAndInvokeV2(TagResourceRequest.builder()
                .tags(tagsToSet)
                .resourceARN(resourceArn)
                .build(), shieldClient::tagResource);
        }

        if (tagsToRemove.size() > 0) {
            proxy.injectCredentialsAndInvokeV2(UntagResourceRequest.builder()
                .tagKeys(tagsToRemove)
                .resourceARN(resourceArn)
                .build(), shieldClient::untagResource);
        }
    }

    public static String protectionArnToId(@NonNull final String protectionArn) {
        final int index = protectionArn.indexOf('/');
        return protectionArn.substring(index + 1);
    }
}
