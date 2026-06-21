package lt.pauliusbaksys.datavault.scheduler;

import lt.pauliusbaksys.datavault.service.ConditionalReleaseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConditionalReleaseScheduler {

    private final ConditionalReleaseService conditionalReleaseService;

    public ConditionalReleaseScheduler(ConditionalReleaseService conditionalReleaseService) {
        this.conditionalReleaseService = conditionalReleaseService;
    }

    // Public method because scheduler needs to see it.
    @Scheduled(fixedDelay = 10_000)
    public void processPolicies(){
        conditionalReleaseService.processPendingPolicies();
    }
}
