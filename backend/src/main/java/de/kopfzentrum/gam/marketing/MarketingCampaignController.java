package de.kopfzentrum.gam.marketing;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketing/campaigns")
public class MarketingCampaignController {
    private final MarketingCampaignRepository repo;

    public MarketingCampaignController(MarketingCampaignRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<MarketingCampaign> list() {
        return repo.list();
    }

    @PostMapping
    public MarketingCampaign create(@RequestBody MarketingCampaignRequest request, Authentication authentication) {
        return repo.create(request, username(authentication));
    }

    @PostMapping("/{id}/scan")
    public MarketingCampaign scan(@PathVariable long id, @RequestBody MarketingScanRequest request, Authentication authentication) {
        return repo.scan(id, request, username(authentication));
    }

    @PostMapping("/{id}/scan/undo-last")
    public MarketingCampaign undoLastScan(@PathVariable long id, Authentication authentication) {
        return repo.undoLastScan(id, username(authentication));
    }

    @PostMapping("/{id}/status/{status}")
    public MarketingCampaign status(@PathVariable long id, @PathVariable String status, Authentication authentication) {
        return repo.transition(id, status.toUpperCase(Locale.ROOT), username(authentication));
    }

    @GetMapping("/{id}/history")
    public List<Map<String, Object>> history(@PathVariable long id) {
        return repo.history(id);
    }

    private String username(Authentication authentication) {
        return authentication != null && authentication.getName() != null
            ? authentication.getName()
            : "system";
    }
}
