package uk.gov.hmcts.reform.civil.sampledata;

import lombok.Data;
import uk.gov.hmcts.reform.civil.model.RoleAssignmentResponse;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Data
public class RoleAssignmentBuilder {

    private Long id;
    private String actorIdType;
    private String actorId;
    private String roleType;
    private String roleName;
    private String classification;
    private String grantType;
    private String roleCategory;
    private boolean readOnly;
    private LocalDate created;
    private RoleAssignmentResponse.RoleAssignment.Attributes attributes;
    private List<String> authorisations;


    public RoleAssignmentBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public RoleAssignmentBuilder actorIdType(String actorIdType) {
        this.actorIdType = actorIdType;
        return this;
    }

    public RoleAssignmentBuilder actorId(String actorId) {
        this.actorId = actorId;
        return this;
    }

    public RoleAssignmentBuilder roleType(String roleType) {
        this.roleType = roleType;
        return this;
    }

    public RoleAssignmentBuilder roleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    public RoleAssignmentBuilder classification(String classification) {
        this.classification = classification;
        return this;
    }

    public RoleAssignmentBuilder grantType(String grantType) {
        this.grantType = grantType;
        return this;
    }

    public RoleAssignmentBuilder roleCategory(String roleCategory) {
        this.roleCategory = roleCategory;
        return this;
    }

    public RoleAssignmentBuilder readonly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public RoleAssignmentBuilder created(LocalDate created) {
        this.created = created;
        return this;
    }

    public RoleAssignmentBuilder authorisations(List<String> authorisations) {
        this.authorisations = authorisations;
        return this;
    }


    public RoleAssignmentResponse.RoleAssignment sampleRoleAssignment() {
        return RoleAssignmentResponse.RoleAssignment.builder()
            .id("b034b492-d2e9-4920-8e72-6cac42e43dfc")
            .actorIdType("IDAM")
            .actorId("1d70e58a-73ab-4423-83b0-9cd82a810250")
            .roleType("ORGANISATION")
            .roleName("senior-tribunal-caseworker")
            .classification("PUBLIC")
            .grantType("STANDARD")
            .created(LocalDate.now())
            .readOnly(false)
            .authorisations(Arrays.asList("QA"))
            .attributes(AttributesBuilder.defaults().build())
            .build();
    }
}
