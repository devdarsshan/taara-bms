package com.taara.bms.config;

import com.taara.bms.service.auth.AppAuthService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final AppAuthService appAuthService;

    public BootstrapAdminInitializer(AppAuthService appAuthService) {
        this.appAuthService = appAuthService;
    }

    @Override
    public void run(ApplicationArguments args) {
        appAuthService.bootstrapAdminIfConfigured();
    }
}
