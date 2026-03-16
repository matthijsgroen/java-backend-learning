package nl.kabisa.dashboarding.widget.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for proxy request execution in widget endpoints.
 * Controls which URLs are allowed to be proxied for security purposes.
 */
@Component
@ConfigurationProperties(prefix = "dashboarding.proxy")
public class ProxyConfiguration {

    /**
     * List of allowed URI schemes for proxy requests.
     * Only URLs matching these schemes will be allowed.
     * Examples: https, http
     * Default: https
     */
    private List<String> allowedSchemes = new ArrayList<>();

    /**
     * List of allowed hostnames for proxy requests.
     * Only URLs with these hostnames will be allowed.
     * Private IP addresses (127.x.x.x, 10.x.x.x, 192.168.x.x, etc.) are always blocked.
     * Examples: calendar.google.com, api.example.com
     * Default: calendar.google.com
     */
    private List<String> allowedHosts = new ArrayList<>();

    public List<String> getAllowedSchemes() {
        return allowedSchemes;
    }

    public void setAllowedSchemes(List<String> allowedSchemes) {
        this.allowedSchemes = allowedSchemes;
    }

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }
}
