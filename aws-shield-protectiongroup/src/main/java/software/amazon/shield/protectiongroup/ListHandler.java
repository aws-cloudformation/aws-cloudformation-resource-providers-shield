package software.amazon.shield.protectiongroup;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.ListProtectionGroupsRequest;
import software.amazon.awssdk.services.shield.model.ListProtectionGroupsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.common.HandlerHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        final List<ResourceModel> models = new ArrayList<>();

        try {
            final ListProtectionGroupsRequest listProtectionGroupsRequest =
                    ListProtectionGroupsRequest.builder()
                            .nextToken(request.getNextToken())
                            .build();

            final ListProtectionGroupsResponse listProtectionGroupsResponse =
                    proxy.injectCredentialsAndInvokeV2(listProtectionGroupsRequest, this.client::listProtectionGroups);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(translateResponse(listProtectionGroupsResponse, proxy))
                    .status(OperationStatus.SUCCESS)
                    .build();

        } catch (RuntimeException e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .build();
        }
    }

    private List<ResourceModel> translateResponse(
            final ListProtectionGroupsResponse response,
            final AmazonWebServicesClientProxy proxy) {
        return Optional.ofNullable(response.protectionGroups())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(
                        protectionGroup ->
                                ResourceModel.builder()
                                        .protectionGroupId(protectionGroup.protectionGroupId())
                                        .aggregation(protectionGroup.aggregationAsString())
                                        .members(protectionGroup.members())
                                        .pattern(protectionGroup.patternAsString())
                                        .protectionGroupArn(protectionGroup.protectionGroupArn())
                                        .resourceType(protectionGroup.resourceTypeAsString())
                                        .tags(
                                                HandlerHelper.getTags(
                                                        proxy,
                                                        this.client,
                                                        protectionGroup.protectionGroupArn(),
                                                        tag ->
                                                                Tag.builder()
                                                                        .key(tag.key())
                                                                        .value(tag.value())
                                                                        .build()))
                                        .build())
                .collect(Collectors.toList());
    }
}
