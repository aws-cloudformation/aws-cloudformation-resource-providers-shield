package software.amazon.shield.protectiongroup;

import java.util.List;

import software.amazon.cloudformation.proxy.StdCallbackContext;
import software.amazon.shield.common.HandlerHelper.TagsContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext implements TagsContext<Tag> {
    private List<Tag> tags;
}
