package software.amazon.shield.protection.helper;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import software.amazon.shield.protection.Action;
import software.amazon.shield.protection.ApplicationLayerAutomaticResponseConfiguration;
import software.amazon.shield.protection.ResourceModel;
import software.amazon.shield.protection.Tag;

public class ProtectionTestData {
    public static final String NAME_1 = "TEST_NAME_cloudfront";
    public static final String NAME_2 = "TEST_NAME_alb";
    public static final String ACCOUNT_ID = "123456789012";
    public static final String RESOURCE_ARN_1 = "arn:aws:cloudfront::" + ACCOUNT_ID + ":distribution/A12B3CD4E5FG67";
    public static final String RESOURCE_ARN_2 =
            "arn:aws:elasticloadbalancing:us-east-1:" + ACCOUNT_ID + ":loadbalancer/app/" + NAME_2 + "/12a3bc4567de890f";

    public static final String PROTECTION_ID = "TEST_PROTECTION_ID";
    public static final String PROTECTION_ARN_TEMPLATE =
            "arn:aws:shield::123456789012:protection/";
    public static final String PROTECTION_ARN = PROTECTION_ARN_TEMPLATE + PROTECTION_ID;

    public static final String NEXT_TOKEN = "TEST_NEXT_TOKEN";

    public static final String HEALTH_CHECK_ARN_TEMPLATE = "arn:aws:route53:::healthcheck/";
    public static final String HEALTH_CHECK_ID_1 = "healthCheckId1";
    public static final String HEALTH_CHECK_ID_2 = "healthCheckId2";
    public static final List<String> HEALTH_CHECK_ARNS =
            Lists.newArrayList(
                    HEALTH_CHECK_ARN_TEMPLATE + HEALTH_CHECK_ID_1,
                    HEALTH_CHECK_ARN_TEMPLATE + HEALTH_CHECK_ID_2);

    public static final Tag TAG_1 = Tag.builder().key("k1").value("v1").build();
    public static final Tag TAG_2 = Tag.builder().key("k2").value("v2").build();
    public static final List<Tag> TAGS = Lists.newArrayList(TAG_1, TAG_2);

    public static final String ENABLED = "ENABLED";
    public static final ApplicationLayerAutomaticResponseConfiguration APP_LAYER_AUTO_RESPONSE_CONFIG =
            ApplicationLayerAutomaticResponseConfiguration.builder()
                    .action(
                            Action.builder()
                                    .block(Maps.newHashMap())
                                    .build())
                    .status(ENABLED)
                    .build();

    public static final ResourceModel RESOURCE_MODEL_1 =
            ResourceModel.builder()
                    .name(NAME_1)
                    .protectionId(PROTECTION_ID)
                    .protectionArn(PROTECTION_ARN)
                    .resourceArn(RESOURCE_ARN_1)
                    .tags(TAGS)
                    .healthCheckArns(HEALTH_CHECK_ARNS)
                    .applicationLayerAutomaticResponseConfiguration(APP_LAYER_AUTO_RESPONSE_CONFIG)
                    .build();

    public static final ResourceModel RESOURCE_MODEL_2 =
            ResourceModel.builder()
                    .name(NAME_2)
                    .protectionId(PROTECTION_ID)
                    .protectionArn(PROTECTION_ARN)
                    .resourceArn(RESOURCE_ARN_2)
                    .tags(TAGS)
                    .healthCheckArns(HEALTH_CHECK_ARNS)
                    .applicationLayerAutomaticResponseConfiguration(APP_LAYER_AUTO_RESPONSE_CONFIG)
                    .build();
    }
