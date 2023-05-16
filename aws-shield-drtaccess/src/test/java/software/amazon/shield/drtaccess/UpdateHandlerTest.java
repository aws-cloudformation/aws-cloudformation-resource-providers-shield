package software.amazon.shield.drtaccess;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;
import software.amazon.shield.drtaccess.helper.DrtAccessTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends DrtAccessTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ResourceModel prevStateResourceModel;

    private UpdateHandler updateHandler;

    private final String roleArn = "update-test";

    private final List<String> logBucketList = Arrays.asList("update-bucket-A", "update-bucket-B", "update-bucket-C");

    @BeforeEach
    public void setup() {
        proxy = spy(new AmazonWebServicesClientProxy(new LoggerProxy(),
            new Credentials("accessKey", "secretKey", "token"),
            () -> Duration.ofSeconds(600).toMillis()));
        logger = mock(Logger.class, withSettings().verboseLogging());
        prevStateResourceModel = getTestResourceModel();
        updateHandler = new UpdateHandler(mock(ShieldClient.class));
        ShieldAPIChainableRemoteCall.JITTER_SECONDS = 0;
    }

    @Test
    public void handleRequest_UpdateBoth() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(accountId)
            .previousResourceState(prevStateResourceModel)
            .desiredResourceState(ResourceModel.builder()
                .accountId(accountId)
                .roleArn(roleArn)
                .logBucketList(logBucketList)
                .build())
            .build();

        mockDisassociateDrtLogBucket(proxy);
        mockAssociateDrtRole(proxy);
        mockAssociateDrtLogBucket(proxy);

        final ProgressEvent<ResourceModel, CallbackContext> response
            = updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRoleArn()).isEqualTo(roleArn);
        assertThat(response.getResourceModel().getLogBucketList().size()).isEqualTo(3);
        assertThat(response.getResourceModel().getLogBucketList().get(0)).isEqualTo(logBucketList.get(0));
        assertThat(response.getResourceModel().getLogBucketList().get(1)).isEqualTo(logBucketList.get(1));
        assertThat(response.getResourceModel().getLogBucketList().get(2)).isEqualTo(logBucketList.get(2));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_OnlyUpdateDrtRole() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(accountId)
            .previousResourceState(prevStateResourceModel)
            .desiredResourceState(ResourceModel.builder()
                .accountId(accountId)
                .roleArn(roleArn)
                .logBucketList(prevStateResourceModel.getLogBucketList())
                .build())
            .build();

        mockAssociateDrtRole(proxy);

        final ProgressEvent<ResourceModel, CallbackContext> response
            = updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRoleArn()).isEqualTo(roleArn);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_OnlyUpdateDrtLogBucket() {

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(accountId)
            .previousResourceState(prevStateResourceModel)
            .desiredResourceState(ResourceModel.builder()
                .accountId(accountId)
                .logBucketList(logBucketList)
                .build())
            .build();

        mockDisassociateDrtLogBucket(proxy);
        mockAssociateDrtLogBucket(proxy);

        final ProgressEvent<ResourceModel, CallbackContext> response
            = updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getLogBucketList().size()).isEqualTo(3);
        assertThat(response.getResourceModel().getLogBucketList().get(0)).isEqualTo(logBucketList.get(0));
        assertThat(response.getResourceModel().getLogBucketList().get(1)).isEqualTo(logBucketList.get(1));
        assertThat(response.getResourceModel().getLogBucketList().get(2)).isEqualTo(logBucketList.get(2));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_EmptyUpdateRequestShouldFail() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(accountId)
            .previousResourceState(prevStateResourceModel)
            .desiredResourceState(ResourceModel.builder().accountId(accountId).build())
            .nextToken("randomNextToken")
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_AccountNotFoundFailure() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(prevStateResourceModel)
            .desiredResourceState(
                ResourceModel.builder()
                    .accountId(accountId)
                    .build()
            )
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
