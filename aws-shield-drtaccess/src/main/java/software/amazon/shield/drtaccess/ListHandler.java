package software.amazon.shield.drtaccess;

import java.util.Collections;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.CustomerAPIClientBuilder;
import software.amazon.shield.drtaccess.helper.HandlerHelper;

@RequiredArgsConstructor
public class ListHandler extends BaseHandler<CallbackContext> {
    private final ShieldClient shieldClient;

    public ListHandler() {
        this.shieldClient = CustomerAPIClientBuilder.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger
    ) {
        logger.log(String.format(
                "ListHandler: DRTAccess AccountID = %s, ClientToken = %s",
                request.getAwsAccountId(),
                request.getClientRequestToken()
            )
        );
        return HandlerHelper.describeDrtAccessSetContext(
            "ListHandler",
            proxy,
            proxy.newProxy(() -> shieldClient),
            request.getDesiredResourceState(),
            callbackContext,
            logger
        ).then(progress -> {
            if (!HandlerHelper.isDrtAccessConfigured(
                progress.getCallbackContext().getRoleArn(),
                progress.getCallbackContext().getLogBucketList()
            )) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Collections.emptyList())
                    .status(OperationStatus.SUCCESS)
                    .build();
            }
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(ImmutableList.of(
                    ResourceModel.builder()
                        .accountId(request.getAwsAccountId())
                        .build()
                ))
                .status(OperationStatus.SUCCESS)
                .build();
        });
    }
}
