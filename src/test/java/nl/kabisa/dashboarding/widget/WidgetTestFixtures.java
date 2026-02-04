package nl.kabisa.dashboarding.widget;

public final class WidgetTestFixtures {
    
    private WidgetTestFixtures() {
        // Utility class - prevent instantiation
    }
    
    public static final String MINIMAL_WIDGET_JSON = """
            {
                "widgetType": "google-calendar-widget",
                "version": "1.0.0",
                "configuration": {},
                "configurationModel": [],
                "dataEndpoints": []
            }
            """;
    
    public static final String WIDGET_WITH_FRONTEND_CONFIG_JSON = """
            {
                "widgetType": "google-calendar-widget",
                "version": "1.0.0",
                "configuration": {
                    "title": "Lunch & Learn binnenkort",
                    "lookAhead": 60
                },
                "configurationModel": [{
                    "id": "title",
                    "type": "string",
                    "scope": "frontend"
                }, {
                    "id": "lookAhead",
                    "type": "integer",
                    "scope": "frontend"
                }],
                "dataEndpoints": []
            }
            """;
    
    public static final String FULL_WIDGET_JSON = """
            {
                "widgetType": "google-calendar-widget",
                "version": "1.0.0",
                "configuration": {
                    "title": "Lunch & Learn binnenkort",
                    "secretIcalUrl": "https://.....",
                    "lookAhead": 60,
                    "lookBack": 0
                },
                "configurationModel": [{
                    "id": "title",
                    "type": "string",
                    "scope": "frontend"
                }, {
                    "id": "secretIcalUrl",
                    "type": "string",
                    "scope": "backend"
                }, {
                    "id": "lookAhead",
                    "type": "integer",
                    "scope": "frontend"
                }, {
                    "id": "lookBack",
                    "type": "integer",
                    "scope": "frontend"
                }],
                "dataEndpoints": [{
                    "path": "calendar",
                    "cache": 600000,
                    "steps": [{
                        "action": "tunnelRequest",
                        "config": {
                            "method": "GET",
                            "url": "%secretIcalUrl%"
                        }
                    }]
                }]
            }
            """;
    
    public static final String INVALID_WIDGET_JSON = """
            {
                "widgetType": "",
                "version": "1.0.0",
                "configuration": {},
                "configurationModel": []
            }
            """;
}
