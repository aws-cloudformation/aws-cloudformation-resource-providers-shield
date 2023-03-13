package software.amazon.shield.proactiveengagement.helper;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.shield.proactiveengagement.EmergencyContact;

public class ProactiveEngagementTestHelper {
    public static String name = "testProactiveEngagement";
    public static String accountId = "shield-unit-test";

    public static Credentials MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");

    public static ProxyClient<ShieldClient> MOCK_PROXY(
            final AmazonWebServicesClientProxy proxy,
            final ShieldClient shieldClient) {
        return new ProxyClient<ShieldClient>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
            injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
            CompletableFuture<ResponseT>
            injectCredentialsAndInvokeV2Async(
                    RequestT request,
                    Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse,
                    IterableT extends SdkIterable<ResponseT>>
            IterableT
            injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
                return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
            injectCredentialsAndInvokeV2InputStream(
                    RequestT requestT,
                    Function<RequestT, ResponseInputStream<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
            injectCredentialsAndInvokeV2Bytes(
                    RequestT requestT,
                    Function<RequestT, ResponseBytes<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ShieldClient client() {
                return shieldClient;
            }
        };
    }

    public static List<software.amazon.awssdk.services.shield.model.EmergencyContact> emergencyContactList =
            Arrays.asList(
            software.amazon.awssdk.services.shield.model.EmergencyContact.builder()
                    .emailAddress("abc@amazon.com")
                    .phoneNumber("+112345")
                    .contactNotes("1st contact")
                    .build(),
            software.amazon.awssdk.services.shield.model.EmergencyContact.builder()
                    .emailAddress("123@amazon.com")
                    .phoneNumber("+154321")
                    .contactNotes("2nd contact")
                    .build());

    public static List<software.amazon.awssdk.services.shield.model.EmergencyContact> newEmergencyContactList =
            Arrays.asList(
            software.amazon.awssdk.services.shield.model.EmergencyContact.builder()
                    .emailAddress("new_abc@amazon.com")
                    .phoneNumber("+11122334455")
                    .contactNotes("new_1st contact")
                    .build(),
            software.amazon.awssdk.services.shield.model.EmergencyContact.builder()
                    .emailAddress("new_123@amazon.com")
                    .phoneNumber("+15544332211")
                    .contactNotes("new_2nd contact")
                    .build());

    public static List<EmergencyContact> convertEmergencyContactList(List<software.amazon.awssdk.services.shield.model.EmergencyContact> emergencyContactList) {
        return Optional.ofNullable(emergencyContactList)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(con -> EmergencyContact.builder().phoneNumber(con.phoneNumber())
                        .emailAddress(con.emailAddress())
                        .contactNotes(con.contactNotes())
                        .build())
                .collect(Collectors.toList());
    }
}
