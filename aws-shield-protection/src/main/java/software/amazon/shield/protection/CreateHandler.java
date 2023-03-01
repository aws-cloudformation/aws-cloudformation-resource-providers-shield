package software.amazon.shield.protection;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.CreateProtectionRequest;
import software.amazon.awssdk.services.shield.model.CreateProtectionResponse;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;

@RequiredArgsConstructor
public class CreateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public CreateHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        try {
            final CreateProtectionRequest.Builder createProtectionRequest =
                    CreateProtectionRequest.builder()
                            .name(model.getName())
                            .resourceArn(model.getResourceArn());

            if (!CollectionUtils.isNullOrEmpty(model.getTags())) {
                createProtectionRequest.tags(
                        model.getTags()
                                .stream()
                                .map(tag ->
                                        Tag.builder()
                                                .key(tag.getKey())
                                                .value(tag.getValue())
                                                .build())
                                .collect(Collectors.toList())
                );
            }

            final CreateProtectionResponse createProtectionResponse =
                    proxy.injectCredentialsAndInvokeV2(createProtectionRequest.build(), this.client::createProtection);

            final ResourceModel readResourceModel =
                    ResourceModel.builder()
                            .protectionId(createProtectionResponse.protectionId())
                            .build();

            return new ReadHandler(this.client).handleRequest(
                    proxy,
                    ResourceHandlerRequest.<ResourceModel>builder()
                            .desiredResourceState(readResourceModel)
                            .build(),
                    null,
                    logger);

        } catch (RuntimeException e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(ExceptionConverter.convertToErrorCode(e))
                    .message(e.getMessage())
                    .build();
        }
    }
}
