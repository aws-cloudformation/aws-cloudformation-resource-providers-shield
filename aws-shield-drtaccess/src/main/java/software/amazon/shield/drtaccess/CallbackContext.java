package software.amazon.shield.drtaccess;

import java.util.List;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private List<String> logBucketList;
    private String roleArn;
}
