package software.amazon.shield.drtaccess.helper;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateDrtLogBucketRequest;
import software.amazon.awssdk.services.shield.model.AssociateDrtLogBucketResponse;
import software.amazon.awssdk.services.shield.model.AssociateDrtRoleRequest;
import software.amazon.awssdk.services.shield.model.AssociateDrtRoleResponse;
import software.amazon.awssdk.services.shield.model.DisassociateDrtLogBucketRequest;
import software.amazon.awssdk.services.shield.model.DisassociateDrtLogBucketResponse;
import software.amazon.awssdk.services.shield.model.DisassociateDrtRoleRequest;
import software.amazon.awssdk.services.shield.model.DisassociateDrtRoleResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

import java.util.List;

public class HandlerHelper {

    public static final String DRTACCESS_CONFLICT_ERROR_MSG = "Your account has SRT role and log bucket associated.";

    public static DisassociateDrtLogBucketResponse disassociateDrtLogBucket(AmazonWebServicesClientProxy proxy, ShieldClient client, String logBucket) {
        DisassociateDrtLogBucketRequest disassociateDrtLogBucketRequest = DisassociateDrtLogBucketRequest.builder()
                .logBucket(logBucket)
                .build();
        return proxy.injectCredentialsAndInvokeV2(
                disassociateDrtLogBucketRequest, client::disassociateDRTLogBucket
        );
    }

    public static void disassociateDrtLogBucketList(AmazonWebServicesClientProxy proxy, ShieldClient client, List<String> logBucketList) {
        if (logBucketList == null || logBucketList.isEmpty()) return;
        logBucketList.forEach(logBucket -> disassociateDrtLogBucket(proxy, client, logBucket));
    }

    public static DisassociateDrtRoleResponse disassociateDrtRole(AmazonWebServicesClientProxy proxy, ShieldClient client) {
        DisassociateDrtRoleRequest disassociateDrtRoleRequest = DisassociateDrtRoleRequest.builder().build();
        return proxy.injectCredentialsAndInvokeV2(
                disassociateDrtRoleRequest, client::disassociateDRTRole
        );
    }

    public static void associateDrtLogBucketList(AmazonWebServicesClientProxy proxy, ShieldClient client, List<String> logBucketList) {
        if (logBucketList == null || logBucketList.isEmpty()) return;
        logBucketList.forEach(logBucket -> associateDrtLogBucket(proxy, client, logBucket));
    }

    public static AssociateDrtLogBucketResponse associateDrtLogBucket(AmazonWebServicesClientProxy proxy, ShieldClient client, String logBucket) {
        AssociateDrtLogBucketRequest associateDrtLogBucketRequest = AssociateDrtLogBucketRequest.builder()
                .logBucket(logBucket)
                .build();
        return proxy.injectCredentialsAndInvokeV2(
                associateDrtLogBucketRequest, client::associateDRTLogBucket);
    }

    public static AssociateDrtRoleResponse associateDrtRole(AmazonWebServicesClientProxy proxy, ShieldClient client, String roleArn) {
        AssociateDrtRoleRequest associateDrtRoleRequest = AssociateDrtRoleRequest.builder()
                .roleArn(roleArn)
                .build();
        return proxy.injectCredentialsAndInvokeV2(
                associateDrtRoleRequest, client::associateDRTRole);
    }
}