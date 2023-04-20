package software.amazon.shield.drtaccess.helper;

import java.util.List;
import javax.annotation.Nullable;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.AssociateDrtLogBucketRequest;
import software.amazon.awssdk.services.shield.model.AssociateDrtLogBucketResponse;
import software.amazon.awssdk.services.shield.model.AssociateDrtRoleRequest;
import software.amazon.awssdk.services.shield.model.AssociateDrtRoleResponse;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessRequest;
import software.amazon.awssdk.services.shield.model.DescribeDrtAccessResponse;
import software.amazon.awssdk.services.shield.model.DisassociateDrtLogBucketRequest;
import software.amazon.awssdk.services.shield.model.DisassociateDrtLogBucketResponse;
import software.amazon.awssdk.services.shield.model.DisassociateDrtRoleRequest;
import software.amazon.awssdk.services.shield.model.DisassociateDrtRoleResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.drtaccess.ResourceModel;

public class HandlerHelper {

    public static final String DRTACCESS_CONFLICT_ERROR_MSG = "Your account already has SRT role and log bucket associated.";
    public static final String NO_DRTACCESS_ERROR_MSG = "Your account doesn't have SRT role associated.";
    public static final String DRTACCESS_ACCOUNT_ID_NOT_FOUND_ERROR_MSG = "Your account ID is not found.";

    public static final String EMPTY_DRTACCESS_REQUEST = "DRT Access requires least one of roleArn or logBucketList " +
        "to be non-empty.";

    public static boolean isEmptyDrtAccessRequest(String roleArn, List<String> logBucketList) {
        return (roleArn == null || roleArn.isEmpty()) && (logBucketList == null || logBucketList.isEmpty());
    }

    public static boolean isDrtAccessConfigured(
        @Nullable String roleArn,
        @Nullable List<String> logBucketList
    ) {
        return (roleArn != null && !roleArn.isEmpty()) || (logBucketList != null && !logBucketList.isEmpty());
    }

    public static boolean accountIdMatchesResourcePrimaryId(ResourceHandlerRequest<ResourceModel> request) {
        return request.getAwsAccountId() != null && request.getDesiredResourceState()
            .getPrimaryIdentifier()
            .getString("/properties/AccountId")
            .equals(request.getAwsAccountId());
    }

    public static DescribeDrtAccessResponse getDrtAccessDescribeResponse(
        AmazonWebServicesClientProxy proxy,
        ShieldClient client,
        Logger logger) {
        logger.log("Starting to subscribe DRT access.");
        DescribeDrtAccessResponse describeDrtAccessResponse = proxy.injectCredentialsAndInvokeV2(
            DescribeDrtAccessRequest.builder().build(), client::describeDRTAccess);
        logger.log("Succeed subscribing DRT access.");
        return describeDrtAccessResponse;
    }

    public static DisassociateDrtLogBucketResponse disassociateDrtLogBucket(
        AmazonWebServicesClientProxy proxy,
        ShieldClient client,
        String logBucket,
        Logger logger) {
        logger.log("Starting to disassociate DRT log bucket " + logBucket);
        DisassociateDrtLogBucketRequest disassociateDrtLogBucketRequest = DisassociateDrtLogBucketRequest.builder()
            .logBucket(logBucket)
            .build();
        DisassociateDrtLogBucketResponse disassociateDrtLogBucketResponse = proxy.injectCredentialsAndInvokeV2(
            disassociateDrtLogBucketRequest, client::disassociateDRTLogBucket
        );
        logger.log("Succeed disassociating DRT log bucket " + logBucket);
        return disassociateDrtLogBucketResponse;
    }

    public static void disassociateDrtLogBucketList(
        AmazonWebServicesClientProxy proxy,
        ShieldClient client,
        List<String> logBucketList,
        Logger logger) {
        if (logBucketList == null || logBucketList.isEmpty()) {
            return;
        }
        logger.log("Starting to disassociate DRT log bucket list with size ." + logBucketList.size());
        logBucketList.forEach(logBucket -> disassociateDrtLogBucket(proxy, client, logBucket, logger));
        logger.log("Succeed disassociating DRT log bucket list.");
    }

    public static DisassociateDrtRoleResponse disassociateDrtRole(
        AmazonWebServicesClientProxy proxy,
        ShieldClient client,
        Logger logger) {
        logger.log("Starting to disassociate DRT role");
        DisassociateDrtRoleRequest disassociateDrtRoleRequest = DisassociateDrtRoleRequest.builder().build();
        DisassociateDrtRoleResponse disassociateDrtRoleResponse = proxy.injectCredentialsAndInvokeV2(
            disassociateDrtRoleRequest, client::disassociateDRTRole
        );
        logger.log("Succeed disassociating DRT role");
        return disassociateDrtRoleResponse;
    }

    public static void associateDrtLogBucketList(
        AmazonWebServicesClientProxy proxy,
        ShieldClient client,
        List<String> logBucketList,
        Logger logger) {
        if (logBucketList == null || logBucketList.isEmpty()) {
            return;
        }
        logger.log("Starting to associate DRT log bucket list with size ." + logBucketList.size());
        logBucketList.forEach(logBucket -> associateDrtLogBucket(proxy, client, logBucket, logger));
        logger.log("Succeed associating DRT log bucket list.");
    }

    public static AssociateDrtLogBucketResponse associateDrtLogBucket(
        AmazonWebServicesClientProxy proxy,
        ShieldClient client,
        String logBucket,
        Logger logger) {
        logger.log("Starting to associate DRT log bucket " + logBucket);
        AssociateDrtLogBucketRequest associateDrtLogBucketRequest = AssociateDrtLogBucketRequest.builder()
            .logBucket(logBucket)
            .build();
        AssociateDrtLogBucketResponse associateDrtLogBucketResponse = proxy.injectCredentialsAndInvokeV2(
            associateDrtLogBucketRequest, client::associateDRTLogBucket);
        logger.log("Succeed associating DRT log bucket " + logBucket);
        return associateDrtLogBucketResponse;
    }

    public static void associateDrtRole(
        AmazonWebServicesClientProxy proxy,
        ShieldClient client,
        String roleArn,
        Logger logger) {
        if (roleArn == null || roleArn.isEmpty()) {
            return;
        }
        logger.log("Starting to associate DRT role " + roleArn);
        AssociateDrtRoleRequest associateDrtRoleRequest = AssociateDrtRoleRequest.builder()
            .roleArn(roleArn)
            .build();
        AssociateDrtRoleResponse associateDrtRoleResponse = proxy.injectCredentialsAndInvokeV2(
            associateDrtRoleRequest, client::associateDRTRole);
        logger.log("Succeed associating DRT role " + roleArn);
    }
}
