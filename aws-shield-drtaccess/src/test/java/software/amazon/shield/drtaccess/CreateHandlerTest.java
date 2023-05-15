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
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends DrtAccessTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler createHandler;

    @BeforeEach
    public void setup() {
        proxy = spy(new AmazonWebServicesClientProxy(new LoggerProxy(),
            new Credentials("accessKey", "secretKey", "token"),
            () -> Duration.ofSeconds(600).toMillis()));
        logger = mock(Logger.class, withSettings().verboseLogging());
        createHandler = new CreateHandler(mock(ShieldClient.class));
    }

    @Test
    public void handleRequest_CreateDrtRoleAndDrtLogBucket() {
        ResourceModel resourceModel = ResourceModel.builder()
            .roleArn(roleArn)
            .logBucketList(logBucketList)
            .build();

        final DescribeDrtAccessResponse describeDrtAccessResponseInitial = DescribeDrtAccessResponse.builder().build();
        final DescribeDrtAccessResponse describeDrtAccessResponseAfterCreate = DescribeDrtAccessResponse.builder()
            .roleArn(roleArn)
            .logBucketList(logBucketList)
            .build();
        doReturn(
            describeDrtAccessResponseInitial,
            describeDrtAccessResponseAfterCreate
        ).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(accountId)
            .desiredResourceState(resourceModel)
            .nextToken("randomNextToken")
            .build();

        mockAssociateDrtRole(proxy);
        mockAssociateDrtLogBucket(proxy);

        final ProgressEvent<ResourceModel, CallbackContext> response
            = createHandler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getAccountId()).isEqualTo(accountId);
        assertThat(response.getResourceModel().getRoleArn()).isEqualTo(roleArn);
        assertThat(response.getResourceModel().getLogBucketList().size()).isEqualTo(2);
        assertThat(response.getResourceModel().getLogBucketList()).isEqualTo(logBucketList);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    }

    @Test
    public void handleRequest_CreateDrtRole() {
        final ResourceModel resourceModel = ResourceModel.builder()
            .roleArn(roleArn)
            .build();

        final DescribeDrtAccessResponse describeDrtAccessResponseInitial = DescribeDrtAccessResponse.builder().build();
        final DescribeDrtAccessResponse describeDrtAccessResponseAfterCreate = DescribeDrtAccessResponse.builder()
            .roleArn(roleArn)
            .build();
        doReturn(
            describeDrtAccessResponseInitial,
            describeDrtAccessResponseAfterCreate
        ).when(proxy).injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(accountId)
            .desiredResourceState(resourceModel)
            .nextToken("randomNextToken")
            .build();

        mockAssociateDrtRole(proxy);

        final ProgressEvent<ResourceModel, CallbackContext> response
            = createHandler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRoleArn()).isEqualTo(roleArn);
    }

    @Test
    public void handleRequest_ResourceConflict() {
        final ResourceModel resourceModel = ResourceModel.builder()
            .roleArn(roleArn)
            .build();

        final DescribeDrtAccessResponse describeDrtAccessResponse = DescribeDrtAccessResponse.builder()
            .roleArn("abc")
            .build();
        doReturn(describeDrtAccessResponse).when(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeDrtAccessRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(accountId)
            .desiredResourceState(resourceModel)
            .nextToken("randomNextToken")
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = createHandler.handleRequest(proxy,
            request,
            new CallbackContext(),
            logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isNotNull();
    }
}
