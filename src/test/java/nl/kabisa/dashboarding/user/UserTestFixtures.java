package nl.kabisa.dashboarding.user;

public final class UserTestFixtures {

    private UserTestFixtures() {
        // Utility class - prevent instantiation
    }

    public static final String REGISTER_USER_JSON = """
            {
                "username": "johndoe",
                "email": "john@example.com",
                "password": "s3cureP@ss!"
            }
            """;

    public static final String REGISTER_USER_2_JSON = """
            {
                "username": "janedoe",
                "email": "jane@example.com",
                "password": "an0therP@ss!"
            }
            """;

    public static final String REGISTER_DUPLICATE_USERNAME_JSON = """
            {
                "username": "johndoe",
                "email": "different@example.com",
                "password": "s3cureP@ss!"
            }
            """;

    public static final String REGISTER_DUPLICATE_EMAIL_JSON = """
            {
                "username": "differentuser",
                "email": "john@example.com",
                "password": "s3cureP@ss!"
            }
            """;

    public static final String REGISTER_INVALID_BLANK_USERNAME_JSON = """
            {
                "username": "",
                "email": "john@example.com",
                "password": "s3cureP@ss!"
            }
            """;

    public static final String REGISTER_INVALID_EMAIL_JSON = """
            {
                "username": "johndoe",
                "email": "not-an-email",
                "password": "s3cureP@ss!"
            }
            """;

    public static final String REGISTER_BLANK_PASSWORD_JSON = """
            {
                "username": "johndoe",
                "email": "john@example.com",
                "password": ""
            }
            """;
}
