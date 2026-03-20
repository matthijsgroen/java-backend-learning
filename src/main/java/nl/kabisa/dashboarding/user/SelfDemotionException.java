package nl.kabisa.dashboarding.user;

public class SelfDemotionException extends RuntimeException {

    public SelfDemotionException() {
        super("An admin cannot change their own role");
    }
}
