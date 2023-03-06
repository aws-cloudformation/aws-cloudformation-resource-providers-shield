package software.amazon.shield.proactiveengagement.helper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.awssdk.services.shield.ShieldClient;
import software.amazon.awssdk.services.shield.model.DisableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.DisableProactiveEngagementResponse;
import software.amazon.awssdk.services.shield.model.EnableProactiveEngagementRequest;
import software.amazon.awssdk.services.shield.model.EnableProactiveEngagementResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.shield.proactiveengagement.EmergencyContact;

public class HandlerHelper {

    public static List<EmergencyContact> convertSDKEmergencyContactList(List<software.amazon.awssdk.services.shield.model.EmergencyContact> emergencyContactList) {
        return Optional.ofNullable(emergencyContactList)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(con -> EmergencyContact.builder().phoneNumber(con.phoneNumber())
                        .emailAddress(con.emailAddress())
                        .contactNotes(con.contactNotes())
                        .build())
                .collect(Collectors.toList());
    }

    public static List<software.amazon.awssdk.services.shield.model.EmergencyContact> convertCFNEmergencyContactList(
            List<EmergencyContact> emergencyContactList) {
        return Optional.ofNullable(emergencyContactList)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(con -> software.amazon.awssdk.services.shield.model.EmergencyContact.builder()
                        .phoneNumber(con.getPhoneNumber())
                        .emailAddress(con.getEmailAddress())
                        .contactNotes(con.getContactNotes())
                        .build())
                .collect(Collectors.toList());
    }

    public static EnableProactiveEngagementResponse enableProactiveEngagement(
            AmazonWebServicesClientProxy proxy,
            ShieldClient client) {
        EnableProactiveEngagementRequest enableProactiveEngagementRequest = EnableProactiveEngagementRequest.builder()
                .build();
        return proxy.injectCredentialsAndInvokeV2(enableProactiveEngagementRequest, client::enableProactiveEngagement);
    }

    public static DisableProactiveEngagementResponse disableProactiveEngagement(
            AmazonWebServicesClientProxy proxy,
            ShieldClient client) {
        DisableProactiveEngagementRequest disableProactiveEngagementRequest =
                DisableProactiveEngagementRequest.builder()
                .build();
        return proxy.injectCredentialsAndInvokeV2(disableProactiveEngagementRequest,
                client::disableProactiveEngagement);
    }
}
