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
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ResourceModel resourceModel;

    private ReadHandler readHandler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        resourceModel = DrtAccessTestHelper.getTestResourceModel();
        readHandler = new ReadHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
                .roleArn(resourceModel.getRoleArn())
                .logBucketList(resourceModel.getLogBucketList())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().accountId(DrtAccessTestHelper.accountId).build())
                .nextToken("randomNextToken")
                .build();

        doReturn(describeDrtAccessResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());
        final ProgressEvent<ResourceModel, CallbackContext> response =
                readHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getResourceModel().getRoleArn()).isEqualTo(DrtAccessTestHelper.roleArn);
        assertThat(response.getResourceModel().getLogBucketList()).isEqualTo(DrtAccessTestHelper.logBucketList);
    }
}
