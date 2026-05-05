package br.com.hadryan.agro.manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configura o bean JavaMailSender para envio de e-mails transacionais via SMTP.
 *
 * O bean é sempre criado para que o contexto Spring suba normalmente em
 * qualquer perfil — inclusive dev e testes, onde spring.mail.host não está
 * configurado. Nesse caso os valores padrão apontam para localhost:1025
 * (Mailpit/Mailhog), e qualquer falha de conexão ao enviar é capturada
 * silenciosamente pelo EmailService.
 */
@Configuration
public class EmailConfig {

    @Value("${spring.mail.host:localhost}")
    private String host;

    @Value("${spring.mail.port:1025}")
    private int port;

    @Value("${spring.mail.username:dev}")
    private String username;

    @Value("${spring.mail.password:dev}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:false}")
    private boolean starttlsRequired;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol",     "smtp");
        props.put("mail.smtp.auth",              smtpAuth);
        props.put("mail.smtp.starttls.enable",   starttlsEnable);
        props.put("mail.smtp.starttls.required", starttlsRequired);
        props.put("mail.debug",                  "false");

        return mailSender;
    }
}