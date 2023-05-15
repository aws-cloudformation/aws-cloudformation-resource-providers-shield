package software.amazon.shield.drtaccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessRequest;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.drtaccess.helper.DrtAccessTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends DrtAccessTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ResourceModel resourceModel;

    private ListHandler listHandler;

    @BeforeEach
    public void setup() {
        proxy = spy(new AmazonWebServicesClientProxy(new LoggerProxy(),
            new Credentials("accessKey", "secretKey", "token"),
            () -> Duration.ofSeconds(600).toMillis()));
        logger = mock(Logger.class);
        listHandler = new ListHandler(mock(ShieldClient.class));
        resourceModel = getTestResourceModel();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
                .roleArn(resourceModel.getRoleArn())
                .logBucketList(resourceModel.getLogBucketList())
                .build();

        doReturn(describeDrtAccessResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceModel model = ResourceModel.builder()
                .accountId(accountId)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(accountId)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                listHandler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()
                .get(0)
                .getAccountId()).isEqualToIgnoringCase(accountId);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NoDrtAccessFailure() {
        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
                .build();

        doReturn(describeDrtAccessResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(accountId)
                .desiredResourceState(ResourceModel.builder().accountId(accountId).build())
                .nextToken("randomNextToken")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                listHandler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels().isEmpty()).isEqualTo(true);
    }
}
