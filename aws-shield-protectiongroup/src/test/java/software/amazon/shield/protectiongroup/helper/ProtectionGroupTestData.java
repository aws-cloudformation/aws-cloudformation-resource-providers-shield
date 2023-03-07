package software.amazon.shield.protectiongroup.helper;

import java.util.List;

import com.google.common.collect.Lists;
import software.amazon.shield.protectiongroup.ResourceModel;
import software.amazon.shield.protectiongroup.Tag;

public class ProtectionGroupTestData {
    public static final String NEXT_TOKEN = "TEST_NEXT_TOKEN";

    public static final String PROTECTION_GROUP_ID = "test_protection_group_id";
    public static final String PROTECTION_GROUP_ARN = "test_protection_group_arn";

    public static final String PATTERN = "test_pattern";
    public static final String AGGREGATION = "test_aggregation";
    public static final String RESOURCE_TYPE = "test_resource_type";

    public static final String MEMBER_1 = "test_member_1";
    public static final String MEMBER_2 = "test_member_2";
    public static final List<String> MEMBERS = Lists.newArrayList(MEMBER_1, MEMBER_2);

    public static final Tag TAG_1 = Tag.builder().key("k1").value("v1").build();
    public static final Tag TAG_2 = Tag.builder().key("k2").value("v2").build();
    public static final List<Tag> TAGS = Lists.newArrayList(TAG_1, TAG_2);

    public static final ResourceModel RESOURCE_MODEL =
            ResourceModel.builder()
                    .pattern(PATTERN)
                    .aggregation(AGGREGATION)
                    .resourceType(RESOURCE_TYPE)
                    .members(MEMBERS)
                    .protectionGroupId(PROTECTION_GROUP_ID)
                    .protectionGroupArn(PROTECTION_GROUP_ARN)
                    .tags(TAGS)
                    .build();
}
