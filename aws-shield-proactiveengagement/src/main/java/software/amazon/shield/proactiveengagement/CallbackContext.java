package software.amazon.shield.proactiveengagement;

import software.amazon.awssdk.services.shield.model.Subscription;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private Subscription subscription;
}
