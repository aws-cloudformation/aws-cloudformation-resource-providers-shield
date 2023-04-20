package software.amazon.shield.drtaccess.helper;

import software.amazon.awssdk.services.shield.model.AssociateDrtLogBucketRequest;
import software.amazon.awssdk.services.shield.model.AssociateDrtLogBucketResponse;
import software.amazon.awssdk.services.shield.model.AssociateDrtRoleRequest;
import software.amazon.awssdk.services.shield.model.AssociateDrtRoleResponse;
import software.amazon.awssdk.services.shield.model.DisassociateDrtLogBucketRequest;
import software.amazon.awssdk.services.shield.model.DisassociateDrtLogBucketResponse;
import software.amazon.awssdk.services.shield.model.DisassociateDrtRoleRequest;
import software.amazon.awssdk.services.shield.model.DisassociateDrtRoleResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.shield.drtaccess.ResourceModel;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

public class DrtAccessTestBase {
    public static String name = "testDrtAccess";
    public static String accountId = "shield-unit-test";
    public static String roleArn = "arn:aws:iam::288146797873:role/service-role/test123";
    public static List<String> logBucketList = Arrays.asList("first-bucket", "second-bucket");

    public static ResourceModel getTestResourceModel() {
        return ResourceModel.builder()
                .accountId(accountId)
                .roleArn(roleArn)
                .logBucketList(logBucketList)
                .build();
    }

    public static void mockAssociateDrtRole(AmazonWebServicesClientProxy proxy) {
        final AssociateDrtRoleResponse associateDrtRoleResponse = AssociateDrtRoleResponse.builder().build();
        doReturn(associateDrtRoleResponse).when(proxy).injectCredentialsAndInvokeV2(any(AssociateDrtRoleRequest.class), any());
    }

    public static void mockDissociateDrtRole(AmazonWebServicesClientProxy proxy) {
        final DisassociateDrtRoleResponse disassociateDrtRoleResponse = DisassociateDrtRoleResponse.builder().build();
        doReturn(disassociateDrtRoleResponse).when(proxy).injectCredentialsAndInvokeV2(any(DisassociateDrtRoleRequest.class), any());
    }

    public static void mockAssociateDrtLogBucket(AmazonWebServicesClientProxy proxy) {
        final AssociateDrtLogBucketResponse associateDrtLogBucketResponse = AssociateDrtLogBucketResponse.builder().build();
        doReturn(associateDrtLogBucketResponse).when(proxy).injectCredentialsAndInvokeV2(any(AssociateDrtLogBucketRequest.class), any());
    }

    public static void mockDisassociateDrtLogBucket(AmazonWebServicesClientProxy proxy) {
        final DisassociateDrtLogBucketResponse disassociateDrtLogBucketResponse = DisassociateDrtLogBucketResponse.builder().build();
        doReturn(disassociateDrtLogBucketResponse).when(proxy).injectCredentialsAndInvokeV2(any(DisassociateDrtLogBucketRequest.class), any());
    }
}
