package software.amazon.shield.proactiveengagement.helper;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
    public static ShieldClient getClient() {
        return ShieldClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
