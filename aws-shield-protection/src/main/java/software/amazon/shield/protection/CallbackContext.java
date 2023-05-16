package software.amazon.shield.protection;

import java.util.List;

import software.amazon.cloudformation.proxy.StdCallbackContext;
import software.amazon.shield.common.HandlerHelper;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext implements HandlerHelper.TagsContext<Tag> {
    private List<Tag> tags;
}
