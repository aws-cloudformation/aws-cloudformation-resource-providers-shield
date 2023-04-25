package software.amazon.shield.protectiongroup;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.CreateProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.Tag;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
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
        final String protectionGroupId = model.getProtectionGroupId();

        logger.log("Starting to create protection group. ");

        try {
            final CreateProtectionGroupRequest.Builder createProtectionGroupRequestBuilder =
                CreateProtectionGroupRequest.builder()
                    .protectionGroupId(protectionGroupId)
                    .aggregation(model.getAggregation())
                    .pattern(model.getPattern());

            if (model.getPattern().equals("ARBITRARY")) {
                createProtectionGroupRequestBuilder.members(model.getMembers());
            } else if (model.getPattern().equals("BY_RESOURCE_TYPE")) {
                createProtectionGroupRequestBuilder.resourceType(model.getResourceType());
            }
            if (!CollectionUtils.isNullOrEmpty(model.getTags())) {
                createProtectionGroupRequestBuilder.tags(
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

            proxy.injectCredentialsAndInvokeV2(
                createProtectionGroupRequestBuilder.build(),
                this.client::createProtectionGroup);

            model.setProtectionGroupArn(String.format("arn:aws:shield::%s:protection-group/%s",
                request.getAwsAccountId(),
                protectionGroupId));

            return new ReadHandler(this.client).handleRequest(proxy, request, callbackContext, logger);
        } catch (RuntimeException e) {
            logger.log("[ERROR] ProtectionGroup CreateHandler: " + e);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(ExceptionConverter.convertToErrorCode(e))
                .message(e.getMessage())
                .build();
        }
    }
}
