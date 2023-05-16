package software.amazon.shield.protectiongroup;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;
import software.amazon.shield.protectiongroup.helper.ProtectionGroupTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private DeleteHandler deleteHandler;
    private ResourceModel resourceModel;

    @BeforeEach
    public void setup() {
        proxy = spy(new AmazonWebServicesClientProxy(new LoggerProxy(),
            new Credentials("accessKey", "secretKey", "token"),
            () -> Duration.ofSeconds(600).toMillis()));
        this.logger = mock(Logger.class);

        this.deleteHandler = new DeleteHandler(mock(ShieldClient.class));
        this.resourceModel = ProtectionGroupTestData.RESOURCE_MODEL;
        ShieldAPIChainableRemoteCall.JITTER_SECONDS = 0;
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(this.resourceModel)
                        .nextToken(ProtectionGroupTestData.NEXT_TOKEN)
                        .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                this.deleteHandler.handleRequest(this.proxy, request, null, this.logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
