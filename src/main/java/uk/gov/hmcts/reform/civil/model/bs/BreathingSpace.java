package uk.gov.hmcts.reform.civil.model.bs;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class BreathingSpace {
    private final String referenceNumber;
    private final LocalDate startDate;
    private final LocalDate endDateEstimated;
    private final LocalDate endDate;
    private final BreathingSpaceType type;
    private final String summaryEnter;
    private final String descriptionEnter;
    private final String summaryLifted;
    private final String descriptionLifted;
    private final BreathingSpaceState state;
}
