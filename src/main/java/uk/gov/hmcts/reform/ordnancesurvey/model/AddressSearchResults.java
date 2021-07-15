package uk.gov.hmcts.reform.ordnancesurvey.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder()
public class AddressSearchResults {

    private Map<String, String> header;
    private List<Dpa> results;
}
