package com.upsjb.ms1.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    public static final String DEFAULT_ENCODING = "UTF-8";

    /*
     * Spring Boot autoconfigura JavaMailSender usando:
     *
     * spring.mail.host
     * spring.mail.port
     * spring.mail.username
     * spring.mail.password
     * spring.mail.properties.*
     *
     * No se crea JavaMailSender manualmente para evitar duplicar
     * configuración y para mantener las credenciales fuera del código.
     *
     * El envío real de correos debe hacerse únicamente desde EmailService.
     */
}