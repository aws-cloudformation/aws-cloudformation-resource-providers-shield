package software.amazon.shield.protectiongroup;

import java.util.List;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.DescribeProtectionGroupResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.ExceptionConverter;
import software.amazon.shield.common.HandlerHelper;

@RequiredArgsConstructor
public class ReadHandler extends BaseHandler<CallbackContext> {

    private final ShieldClient client;

    public ReadHandler() {
        this.client = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final String protectionGroupArn = model.getProtectionGroupArn();
        logger.log(String.format("ReadHandler: protectionGroup arn = %s", protectionGroupArn));
        final String protectionGroupId = HandlerHelper.protectionArnToId(protectionGroupArn);
        logger.log(String.format("ReadHandler: protectionGroup id = %s", protectionGroupId));

        try {
            final DescribeProtectionGroupRequest describeProtectionGroupRequest =
                    DescribeProtectionGroupRequest.builder()
                            .protectionGroupId(protectionGroupId)
                            .build();

            final DescribeProtectionGroupResponse describeProtectionGroupResponse =
                    proxy.injectCredentialsAndInvokeV2(describeProtectionGroupRequest, client::describeProtectionGroup);

            final List<Tag> tags =
                    HandlerHelper.getTags(
                            proxy,
                            client,
                            describeProtectionGroupResponse.protectionGroup().protectionGroupArn(),
                            tag -> Tag.builder()
                                    .key(tag.key())
                                    .value(tag.value())
                                    .build());

            final ResourceModel.ResourceModelBuilder result =
                    ResourceModel.builder()
                            .protectionGroupId(describeProtectionGroupResponse.protectionGroup().protectionGroupId())
                            .protectionGroupArn(describeProtectionGroupResponse.protectionGroup().protectionGroupArn())
                            .pattern(describeProtectionGroupResponse.protectionGroup().patternAsString())
                            .aggregation(describeProtectionGroupResponse.protectionGroup().aggregationAsString())
                            .members(describeProtectionGroupResponse.protectionGroup().members())
                            .resourceType(describeProtectionGroupResponse.protectionGroup().resourceTypeAsString());

            if (!CollectionUtils.isNullOrEmpty(tags)) {
                result.tags(tags);
            }

            return ProgressEvent.defaultSuccessHandler(result.build());

        } catch (RuntimeException e) {
            logger.log("[ERROR] ProtectionGroup ReadHandler: " + e);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(ExceptionConverter.convertToErrorCode(e))
                    .message(e.getMessage())
                    .build();
        }
    }
}
