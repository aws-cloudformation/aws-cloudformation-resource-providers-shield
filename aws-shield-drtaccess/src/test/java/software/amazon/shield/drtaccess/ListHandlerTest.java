package software.amazon.shield.drtaccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ResourceModel resourceModel;

    private ListHandler listHandler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        listHandler = new ListHandler();
        resourceModel = DrtAccessTestHelper.getTestResourceModel();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
                .roleArn(resourceModel.getRoleArn())
                .logBucketList(resourceModel.getLogBucketList())
                .build();

        doReturn(describeDrtAccessResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceModel model = ResourceModel.builder()
                .accountId(DrtAccessTestHelper.accountId)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(DrtAccessTestHelper.accountId)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                listHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels().get(0).getAccountId()).isEqualToIgnoringCase(DrtAccessTestHelper.accountId);
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
                listHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_AccountNotFoundFailure() {
        final ResourceModel model = ResourceModel.builder()
                .accountId(DrtAccessTestHelper.accountId)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                listHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
