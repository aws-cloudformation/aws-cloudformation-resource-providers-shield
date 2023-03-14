package software.amazon.shield.drtaccess;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessRequest;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.drtaccess.helper.DrtAccessTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ResourceModel resourceModel;

    private UpdateHandler updateHandler;

    private final String roleArn = "update-test";

    private final List<String> logBucketList = Arrays.asList("update-bucket-A", "update-bucket-B", "update-bucket-C");

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        resourceModel = DrtAccessTestHelper.getTestResourceModel();
        updateHandler = new UpdateHandler(mock(ShieldClient.class));
    }

    @Test
    public void handleRequest_UpdateBoth() {
        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
                .roleArn(resourceModel.getRoleArn())
                .logBucketList(resourceModel.getLogBucketList())
                .build();
        doReturn(describeDrtAccessResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceModel model = ResourceModel.builder()
                .accountId(DrtAccessTestHelper.accountId)
                .roleArn(roleArn)
                .logBucketList(logBucketList)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(DrtAccessTestHelper.accountId)
                .desiredResourceState(model)
                .build();

        DrtAccessTestHelper.mockDisassociateDrtLogBucket(proxy);
        DrtAccessTestHelper.mockAssociateDrtRole(proxy);
        DrtAccessTestHelper.mockAssociateDrtLogBucket(proxy);

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
        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
                .roleArn(resourceModel.getRoleArn())
                .logBucketList(resourceModel.getLogBucketList())
                .build();
        doReturn(describeDrtAccessResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceModel model = ResourceModel.builder()
                .accountId(DrtAccessTestHelper.accountId)
                .roleArn(roleArn)
                .logBucketList(resourceModel.getLogBucketList())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(DrtAccessTestHelper.accountId)
                .desiredResourceState(model)
                .build();

        DrtAccessTestHelper.mockAssociateDrtRole(proxy);

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
        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
                .roleArn(resourceModel.getRoleArn())
                .logBucketList(resourceModel.getLogBucketList())
                .build();
        doReturn(describeDrtAccessResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceModel model = ResourceModel.builder()
                .accountId(DrtAccessTestHelper.accountId)
                .logBucketList(logBucketList)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(DrtAccessTestHelper.accountId)
                .desiredResourceState(model)
                .build();

        DrtAccessTestHelper.mockDisassociateDrtLogBucket(proxy);
        DrtAccessTestHelper.mockAssociateDrtLogBucket(proxy);

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
    public void handleRequest_NoDrtAccessFailure() {
        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
                .build();

        doReturn(describeDrtAccessResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(DrtAccessTestHelper.accountId)
                .desiredResourceState(ResourceModel.builder().accountId(DrtAccessTestHelper.accountId).build())
                .nextToken("randomNextToken")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_AccountNotFoundFailure() {
        final ListHandler handler = new ListHandler();

        final ResourceModel model = ResourceModel.builder()
                .accountId(DrtAccessTestHelper.accountId)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
