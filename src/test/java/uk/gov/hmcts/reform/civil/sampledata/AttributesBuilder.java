package uk.gov.hmcts.reform.civil.sampledata;

import uk.gov.hmcts.reform.civil.model.RoleAssignmentResponse;

public class AttributesBuilder {
    public static RoleAssignmentResponse.RoleAssignment.Attributes.AttributesBuilder defaults() {
        return RoleAssignmentResponse.RoleAssignment.Attributes.builder()
            .substantive("Y")
            .primaryLocation("123999")
            .jurisdiction("IA");
    }
}
