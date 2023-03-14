package software.amazon.shield.proactiveengagement.helper;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class EventualConsistencyCallbackContext extends StdCallbackContext {
    protected boolean propagationComplete;
}
