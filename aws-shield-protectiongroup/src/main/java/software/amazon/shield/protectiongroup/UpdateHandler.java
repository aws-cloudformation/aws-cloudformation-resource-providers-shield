package software.amazon.shield.protectiongroup;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.UpdateProtectionGroupRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.common.HandlerHelper;

@RequiredArgsConstructor
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public UpdateHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel currentState = request.getPreviousResourceState();
        final ResourceModel desiredState = request.getDesiredResourceState();
        final String protectionGroupArn = desiredState.getProtectionGroupArn();
        logger.log(String.format("UpdateHandler: protectionGroup arn = %s", protectionGroupArn));
        final String protectionGroupId = HandlerHelper.protectionArnToId(protectionGroupArn);
        logger.log(String.format("UpdateHandler: protectionGroup id = %s", protectionGroupId));

        try {
            final UpdateProtectionGroupRequest.Builder updateProtectionGroupRequestBuilder =
                UpdateProtectionGroupRequest.builder()
                    .protectionGroupId(protectionGroupId)
                    .aggregation(desiredState.getAggregation())
                    .members(desiredState.getMembers())
                    .pattern(desiredState.getPattern())
                    .resourceType(desiredState.getResourceType());

            if (desiredState.getPattern().equals("ARBITRARY")) {
                updateProtectionGroupRequestBuilder.members(desiredState.getMembers());
            } else if (desiredState.getPattern().equals("BY_RESOURCE_TYPE")) {
                updateProtectionGroupRequestBuilder.resourceType(desiredState.getResourceType());
            }

            proxy.injectCredentialsAndInvokeV2(updateProtectionGroupRequestBuilder.build(),
                this.client::updateProtectionGroup);

            HandlerHelper.updateTags(
                desiredState.getTags(),
                Tag::getKey,
                Tag::getValue,
                currentState.getTags(),
                Tag::getKey,
                Tag::getValue,
                protectionGroupArn,
                this.client,
                proxy
            );

            return new ReadHandler(this.client).handleRequest(proxy, request, callbackContext, logger);
        } catch (RuntimeException e) {
            logger.log("[ERROR] ProtectionGroup UpdateHandler: " + e);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(ExceptionConverter.convertToErrorCode(e))
                .message(e.getMessage())
                .build();
        }
    }

}
