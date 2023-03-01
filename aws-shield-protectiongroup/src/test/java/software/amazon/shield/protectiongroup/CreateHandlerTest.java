package software.amazon.shield.protectiongroup;

import com.google.common.collect.Lists;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.CreateProtectionGroupRequest;
import software.amazon.awssdk.services.shield.model.CreateProtectionGroupResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private ResourceModel resourceModel;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);

        createHandler = new CreateHandler(mock(ShieldClient.class));
        resourceModel =
                ResourceModel.builder()
                        .pattern("pattern")
                        .aggregation("aggregation")
                        .resourceType("resourceType")
                        .members(Lists.newArrayList("m1"))
                        .protectionGroupId("protectionGroupId")
                        .protectionGroupArn("protectionGroupArn")
                        .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder()
                        .desiredResourceState(resourceModel)
                        .nextToken("nextToken")
                        .build();

        final CreateProtectionGroupResponse createProtectionGroupResponse =
                CreateProtectionGroupResponse.builder()
                        .build();

        doReturn(createProtectionGroupResponse)
                .when(proxy).injectCredentialsAndInvokeV2(any(CreateProtectionGroupRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                createHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        // assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
