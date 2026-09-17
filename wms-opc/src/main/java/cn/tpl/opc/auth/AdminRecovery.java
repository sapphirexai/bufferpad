package cn.tpl.opc.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Local maintenance entrypoint. No web server or device listeners are created. */
public class AdminRecovery {
    @Configuration @EnableAutoConfiguration @org.springframework.context.annotation.Profile("admin-recovery")
    @Import({AuthService.class, LoginAttempts.class})
    public static class RecoveryConfiguration {}

    public static void main(String[] args) {
        String password = System.getenv("BUFFERPAD_RECOVERY_PASSWORD");
        String username = System.getenv().getOrDefault("BUFFERPAD_RECOVERY_USERNAME", "admin");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("A recovery password must be supplied by the local maintenance script");
        SpringApplication application = new SpringApplication(RecoveryConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setAdditionalProfiles("admin-recovery");
        try (ConfigurableApplicationContext context = application.run(args)) {
            context.getBean(AuthService.class).recoverAdministrator(username, password);
            System.out.println("Administrator recovered. Existing sessions revoked; password change is required at next login.");
        }
    }
}
