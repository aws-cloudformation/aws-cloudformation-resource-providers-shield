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
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.shield.common.ShieldAPIChainableRemoteCall;
import software.amazon.shield.drtaccess.CallbackContext;
import software.amazon.shield.drtaccess.ResourceModel;

public class HandlerHelper {

    public static final String DRTACCESS_CONFLICT_ERROR_MSG = "Your account already has SRT role and log bucket " +
        "associated.";
    public static final String NO_DRTACCESS_ERROR_MSG = "Your account doesn't have SRT role associated.";
    public static final String DRTACCESS_ACCOUNT_ID_NOT_FOUND_ERROR_MSG = "Your account ID is not found.";

    public static final String EMPTY_DRTACCESS_REQUEST =
        "DRT Access requires least one of roleArn or logBucketList " + "to be non-empty.";

    public static boolean isEmptyDrtAccessRequest(String roleArn, List<String> logBucketList) {
        return (roleArn == null || roleArn.isEmpty()) && (logBucketList == null || logBucketList.isEmpty());
    }

    public static boolean isDrtAccessConfigured(
        @Nullable String roleArn, @Nullable List<String> logBucketList) {
        return (roleArn != null && !roleArn.isEmpty()) || (logBucketList != null && !logBucketList.isEmpty());
    }

    public static boolean accountIdMatchesResourcePrimaryId(ResourceHandlerRequest<ResourceModel> request) {
        return request.getAwsAccountId() != null && request.getDesiredResourceState()
            .getAccountId()
            .equals(request.getAwsAccountId());
    }

    public static ProgressEvent<ResourceModel, CallbackContext> describeDrtAccessSetContext(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DescribeDrtAccessRequest,
                DescribeDrtAccessResponse>builder()
            .resourceType("DRTAccess")
            .handlerName(handlerName)
            .apiName("describeDRTAccess")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(context)
            .logger(logger)
            .translateToServiceRequest(m -> DescribeDrtAccessRequest.builder().build())
            .getRequestFunction(c -> c::describeDRTAccess)
            .onSuccess((req, res, c, m, ctx) -> {
                ctx.setRoleArn(res.roleArn());
                ctx.setLogBucketList(res.logBucketList());
                return null;
            })
            .build()
            .initiate();
    }

    public static ProgressEvent<ResourceModel, CallbackContext> disassociateDrtLogBucket(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        String logBucket,
        final CallbackContext context,
        final Logger logger
    ) {
        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DisassociateDrtLogBucketRequest,
                DisassociateDrtLogBucketResponse>builder()
            .resourceType("DRTAccess")
            .handlerName(handlerName)
            .apiName("disassociateDRTLogBucket")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(context)
            .logger(logger)
            .translateToServiceRequest(m -> DisassociateDrtLogBucketRequest.builder().logBucket(logBucket).build())
            .getRequestFunction(c -> c::disassociateDRTLogBucket)
            .build()
            .initiate();
    }

    public static ProgressEvent<ResourceModel, CallbackContext> disassociateDrtLogBucketList(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        List<String> logBucketList,
        final CallbackContext context,
        final Logger logger
    ) {
        ProgressEvent<ResourceModel, CallbackContext> ret = ProgressEvent.defaultInProgressHandler(context, 0, model);
        if (logBucketList == null || logBucketList.isEmpty()) {
            return ret;
        }
        for (String logBucket : logBucketList) {
            ret = ret.then(progress -> disassociateDrtLogBucket(
                handlerName,
                proxy,
                proxyClient,
                progress.getResourceModel(),
                logBucket,
                progress.getCallbackContext(),
                logger
            ));
        }
        return ret;
    }

    public static ProgressEvent<ResourceModel, CallbackContext> associateDrtLogBucketList(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        List<String> logBucketList,
        final CallbackContext context,
        final Logger logger
    ) {
        ProgressEvent<ResourceModel, CallbackContext> ret = ProgressEvent.defaultInProgressHandler(context, 0, model);
        if (logBucketList == null || logBucketList.isEmpty()) {
            return ret;
        }
        for (String logBucket : logBucketList) {
            ret = ret.then(progress -> associateDrtLogBucket(
                handlerName,
                proxy,
                proxyClient,
                progress.getResourceModel(),
                logBucket,
                progress.getCallbackContext(),
                logger
            ));
        }
        return ret;
    }

    public static ProgressEvent<ResourceModel, CallbackContext> associateDrtLogBucket(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        String logBucket,
        final CallbackContext context,
        final Logger logger
    ) {
        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, AssociateDrtLogBucketRequest,
                AssociateDrtLogBucketResponse>builder()
            .resourceType("DRTAccess")
            .handlerName(handlerName)
            .apiName("associateDRTLogBucket")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(context)
            .logger(logger)
            .translateToServiceRequest(m -> AssociateDrtLogBucketRequest.builder().logBucket(logBucket).build())
            .getRequestFunction(c -> c::associateDRTLogBucket)
            .build()
            .initiate();
    }

    public static ProgressEvent<ResourceModel, CallbackContext> associateDrtRole(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        String roleArn,
        final CallbackContext context,
        final Logger logger
    ) {
        if (roleArn == null || roleArn.isEmpty()) {
            return ProgressEvent.defaultInProgressHandler(context, 0, model);
        }
        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, AssociateDrtRoleRequest,
                AssociateDrtRoleResponse>builder()
            .resourceType("DRTAccess")
            .handlerName(handlerName)
            .apiName("associateDRTRole")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(context)
            .logger(logger)
            .translateToServiceRequest(m -> AssociateDrtRoleRequest.builder().roleArn(roleArn).build())
            .getRequestFunction(c -> c::associateDRTRole)
            .build()
            .initiate();
    }

    public static ProgressEvent<ResourceModel, CallbackContext> disassociateDrtRole(
        final String handlerName,
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<ShieldClient> proxyClient,
        final ResourceModel model,
        final CallbackContext context,
        final Logger logger
    ) {
        return ShieldAPIChainableRemoteCall.<ResourceModel, CallbackContext, DisassociateDrtRoleRequest,
                DisassociateDrtRoleResponse>builder()
            .resourceType("DRTAccess")
            .handlerName(handlerName)
            .apiName("disassociateDRTRole")
            .proxy(proxy)
            .proxyClient(proxyClient)
            .model(model)
            .context(context)
            .logger(logger)
            .translateToServiceRequest(m -> DisassociateDrtRoleRequest.builder().build())
            .getRequestFunction(c -> c::disassociateDRTRole)
            .build()
            .initiate();
    }
}
