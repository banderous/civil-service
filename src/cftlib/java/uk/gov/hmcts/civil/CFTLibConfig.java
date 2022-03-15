package uk.gov.hmcts.civil;


import org.springframework.stereotype.Component;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class CFTLibConfig implements CFTLibConfigurer {

    @Value("ccd-civil-${CCD_DEF_NAME:dev}.xlsx")
    String defName;

    @Override
    public void configure(CFTLib lib) throws IOException {

        lib.createRoles(
            "caseworker",
            "caseworker-civil",
            "caseworker-civil-solicitor",
            "pui-caa",
            "pui-organisation-manager",
            "pui-case-manager",
            "pui-user-manager",
            "payments"
        );
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        var json = IOUtils.toString(resourceLoader.getResource("classpath:cftlib-am-role-assignments.json")
                                        .getInputStream(), Charset.defaultCharset());
        lib.configureRoleAssignments(json);

        var def = Files.readAllBytes(Path.of("build/ccd-config/" + defName));
        lib.importDefinition(def);
    }
}
