package software.amazon.shield.protection;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ListProtectionsRequest;
import software.amazon.awssdk.services.shield.model.ListProtectionsResponse;
import software.amazon.awssdk.services.shield.model.Protection;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.common.HandlerHelper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ListHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public ListHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        try {
            final ListProtectionsRequest listProtectionsRequest =
                ListProtectionsRequest.builder()
                    .nextToken(request.getNextToken())
                    .build();

            final ListProtectionsResponse listProtectionsResponse =
                proxy.injectCredentialsAndInvokeV2(listProtectionsRequest, this.client::listProtections);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(transformToModels(listProtectionsResponse.protections(), proxy))
                .status(OperationStatus.SUCCESS)
                .build();

        } catch (RuntimeException e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(ExceptionConverter.convertToErrorCode(e))
                .message(e.getMessage())
                .build();
        }
    }

    private List<ResourceModel> transformToModels(
        final List<Protection> protections,
        final AmazonWebServicesClientProxy proxy) {

        return Optional.ofNullable(protections)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(
                protection ->
                    ResourceModel.builder()
                        .protectionId(protection.id())
                        .name(protection.name())
                        .protectionArn(protection.protectionArn())
                        .resourceArn(protection.resourceArn())
                        .tags(
                            HandlerHelper.getTags(
                                proxy,
                                this.client,
                                protection.resourceArn(),
                                tag ->
                                    Tag.builder()
                                        .key(tag.key())
                                        .value(tag.value())
                                        .build()))
                        .build())
            .collect(Collectors.toList());
    }
}
