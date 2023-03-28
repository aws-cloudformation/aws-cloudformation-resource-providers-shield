package software.amazon.shield.protection.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.shield.common.HandlerHelper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class HandlerHelperTest {

    final private String PROTECTION_ARN = "arn:aws:shield::073078365407:protection/a38ad3d7-3968-4743-b929-434da1a460e2";
    final private String PROTECTION_ID = "a38ad3d7-3968-4743-b929-434da1a460e2";

    @Test
    public void protectionArnToIdHappyPath() {
        assertThat(HandlerHelper.protectionArnToId(PROTECTION_ARN)).isEqualTo(PROTECTION_ID);
    }

    @Test
    public void protectionArnToIdWithInvalidArn() {
        assertThat(HandlerHelper.protectionArnToId(PROTECTION_ID)).isEqualTo(PROTECTION_ID);
    }
}
