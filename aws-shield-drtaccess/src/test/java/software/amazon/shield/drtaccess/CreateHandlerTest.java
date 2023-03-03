package software.amazon.shield.drtaccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessRequest;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
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
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler createHandler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        createHandler = new CreateHandler();
    }

    @Test
    public void handleRequest_CreateDrtRoleAndDrtLogBucket() {
        ResourceModel resourceModel = ResourceModel.builder()
                .accountId(DrtAccessTestHelper.accountId)
                .roleArn(DrtAccessTestHelper.roleArn)
                .logBucketList(DrtAccessTestHelper.logBucketList)
                .build();

        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder().build();
        doReturn(describeDrtAccessResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .nextToken("randomNextToken")
                .build();

        DrtAccessTestHelper.mockAssociateDrtRole(proxy);
        DrtAccessTestHelper.mockAssociateDrtLogBucket(proxy);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = createHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRoleArn()).isEqualTo(DrtAccessTestHelper.roleArn);
        assertThat(response.getResourceModel().getLogBucketList().size()).isEqualTo(2);
        assertThat(response.getResourceModel().getLogBucketList()).isEqualTo(DrtAccessTestHelper.logBucketList);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    }

    @Test
    public void handleRequest_CreateDrtRole() {
        final ResourceModel resourceModel = ResourceModel.builder()
                .roleArn(DrtAccessTestHelper.roleArn)
                .build();

        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder().build();
        doReturn(describeDrtAccessResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .nextToken("randomNextToken")
                .build();

        DrtAccessTestHelper.mockAssociateDrtRole(proxy);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = createHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRoleArn()).isEqualTo(DrtAccessTestHelper.roleArn);
    }

    @Test
    public void handleRequest_ResourceConflict() {
        final ResourceModel resourceModel = ResourceModel.builder()
                .roleArn(DrtAccessTestHelper.roleArn)
                .build();

        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
                .roleArn("abc")
                .build();
        doReturn(describeDrtAccessResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .nextToken("randomNextToken")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = createHandler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isNotNull();
    }
}
