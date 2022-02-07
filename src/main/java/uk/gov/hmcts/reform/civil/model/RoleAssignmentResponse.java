package uk.gov.hmcts.reform.civil.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;


import java.time.LocalDate;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentResponse {

    private List<RoleAssignment> roleAssignmentResponse;

    @Data
    @Builder(toBuilder = true)
    public static class RoleAssignment {
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String id;
        private String actorIdType;
        private String actorId;
        private String roleType;
        private String roleName;
        private String classification;
        private String grantType;
        private String roleCategory;
        private boolean readOnly;
        private LocalDate created;
        private Attributes attributes;
        private List<String> authorisations;

        @Data
        @Builder(toBuilder = true)
        public static class Attributes {
            private String substantive;
            private String primaryLocation;
            private String caseId;
            private String jurisdiction;
            private String caseType;
        }
    }
}
