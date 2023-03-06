package software.amazon.shield.proactiveengagement.helper;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.shield.proactiveengagement.EmergencyContact;

public class ProactiveEngagementTestHelper {
    public static String name = "testProactiveEngagement";
    public static String accountId = "shield-unit-test";
    public static List<software.amazon.awssdk.services.shield.model.EmergencyContact> emergencyContactList =
            Arrays.asList(
                    software.amazon.awssdk.services.shield.model.EmergencyContact.builder()
                            .emailAddress("abc@amazon.com")
                            .phoneNumber("12345")
                            .contactNotes("1st contact")
                            .build(),
                    software.amazon.awssdk.services.shield.model.EmergencyContact.builder()
                            .emailAddress("123@amazon.com")
                            .phoneNumber("abcde")
                            .contactNotes("2nd contact")
                            .build());

    public static List<EmergencyContact> convertEmergencyContactList(List<software.amazon.awssdk.services.shield.model.EmergencyContact> emergencyContactList) {
        return Optional.ofNullable(emergencyContactList)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(con -> EmergencyContact.builder().phoneNumber(con.phoneNumber())
                        .emailAddress(con.emailAddress())
                        .contactNotes(con.contactNotes())
                        .build())
                .collect(Collectors.toList());
    }
}
