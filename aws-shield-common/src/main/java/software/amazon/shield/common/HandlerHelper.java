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
import software.amazon.awssdk.services.shield.model.TagResourceResponse;
import software.amazon.awssdk.services.shield.model.UntagResourceRequest;
import software.amazon.awssdk.services.shield.model.UntagResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.StdCallbackContext;

public class HandlerHelper {
    public static <T> List<T> getTags(
        @NonNull final AmazonWebServicesClientProxy proxy,
        @NonNull final ShieldClient client,
        @NonNull final String resourceArn,
        @NonNull final Function<Tag, T> converter) {

        ListTagsForResourceRequest request = ListTagsForResourceRequest.builder().resourceARN(resourceArn).build();

        return proxy.injectCredentialsAndInvokeV2(request, client::listTagsForResource)
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
        @NonNull final AmazonWebServicesClientProxy proxy) {

        final Map<String, String> currentTagsMap = Optional.ofNullable(currentTags)
            .orElse(Collections.emptyList())
            .stream()
            .collect(Collectors.toMap(currentTagKeyGetter, currentTagValueGetter));

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

    public static <T, S, M, C extends StdCallbackContext> ProgressEvent<M, C> updateTagsChainable(
        @Nullable final List<? extends T> desiredTags,
        Function<? super T, String> desiredTagKeyGetter,
        Function<? super T, String> desiredTagValueGetter,
        @Nullable final List<? extends S> currentTags,
        Function<? super S, String> currentTagKeyGetter,
        Function<? super S, String> currentTagValueGetter,
        @NonNull final String resourceArn,

        @NonNull final String resourceType,
        @NonNull final String handlerName,
        @NonNull final AmazonWebServicesClientProxy proxy,
        @NonNull final ProxyClient<ShieldClient> proxyClient,
        @NonNull final M model,
        @NonNull final C callbackContext,
        @NonNull final Logger logger) {
        final Map<String, String> currentTagsMap = Optional.ofNullable(currentTags)
            .orElse(Collections.emptyList())
            .stream()
            .collect(Collectors.toMap(currentTagKeyGetter, currentTagValueGetter));

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

        ProgressEvent<M, C> ret = ProgressEvent.progress(model, callbackContext);

        if (tagsToSet.size() > 0) {
            ret =
                ret.then(progress -> ShieldAPIChainableRemoteCall.<M, C, TagResourceRequest, TagResourceResponse>builder()
                    .resourceType(resourceType)
                    .handlerName(handlerName)
                    .apiName("tagResource")
                    .proxy(proxy)
                    .proxyClient(proxyClient)
                    .model(progress.getResourceModel())
                    .context(progress.getCallbackContext())
                    .logger(logger)
                    .translateToServiceRequest(ignored -> TagResourceRequest.builder()
                        .tags(tagsToSet)
                        .resourceARN(resourceArn)
                        .build())
                    .getRequestFunction(c -> c::tagResource)
                    .build()
                    .initiate());
        }

        if (tagsToRemove.size() > 0) {
            ret = ret.then(progress -> ShieldAPIChainableRemoteCall.<M, C, UntagResourceRequest,
                    UntagResourceResponse>builder()
                .resourceType(resourceType)
                .handlerName(handlerName)
                .apiName("untagResource")
                .proxy(proxy)
                .proxyClient(proxyClient)
                .model(progress.getResourceModel())
                .context(progress.getCallbackContext())
                .logger(logger)
                .translateToServiceRequest(ignored -> UntagResourceRequest.builder()
                    .tagKeys(tagsToRemove)
                    .resourceARN(resourceArn)
                    .build())
                .getRequestFunction(c -> c::untagResource)
                .build()
                .initiate());
        }
        return ret;
    }

    public static String protectionArnToId(@NonNull final String protectionArn) {
        final int index = protectionArn.indexOf('/');
        return protectionArn.substring(index + 1);
    }
}
