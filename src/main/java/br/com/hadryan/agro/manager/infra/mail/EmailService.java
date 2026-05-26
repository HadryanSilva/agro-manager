package br.com.hadryan.agro.manager.infra.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Serviço responsável pelo envio de e-mails transacionais.
 * Os templates HTML ficam em src/main/resources/templates/email/
 * e são processados pelo SpringTemplateEngine do Thymeleaf.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from-address:noreply@agromanager.dev.br}")
    private String fromEmail;

    @Value("${app.mail.from-name:Agro Manager}")
    private String fromName;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    /**
     * Envia o e-mail de convite para ingresso em uma conta.
     * Em caso de falha no envio, o erro é registrado em log sem propagar exceção,
     * garantindo que a criação do convite no banco não seja revertida.
     *
     * @param toEmail     e-mail do destinatário
     * @param accountName nome da conta para qual o usuário foi convidado
     * @param inviterName nome de quem gerou o convite
     * @param role        papel que será atribuído ao aceitar (ADMIN, MEMBER...)
     * @param inviteUrl   URL completa do convite
     * @param expiresAt   data de expiração do convite
     */
    public void sendInviteEmail(String toEmail,
                                String accountName,
                                String inviterName,
                                String role,
                                String inviteUrl,
                                LocalDateTime expiresAt,
                                String inviteCode) {
        try {
            // Monta as variáveis disponíveis no template
            Context context = new Context(Locale.forLanguageTag("pt-BR"));
            context.setVariable("inviterName",  inviterName);
            context.setVariable("accountName",  accountName);
            context.setVariable("roleLabel",    resolveRoleLabel(role));
            context.setVariable("inviteUrl",    inviteUrl);
            context.setVariable("expiresAt",    expiresAt.format(DATE_FORMATTER));
            context.setVariable("inviteCode",   inviteCode);

            // Processa o template email/invite.html com as variáveis acima
            String html = templateEngine.process("email/invite", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Você foi convidado para " + accountName + " — Agro Manager");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("E-mail de convite enviado para {} — conta: {}", toEmail, accountName);

        } catch (MessagingException e) {
            log.error("Falha ao enviar e-mail de convite para {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao enviar e-mail de convite para {}: {}", toEmail, e.getMessage());
        }
    }

    // Traduz o nome do enum para exibição amigável no e-mail
    private String resolveRoleLabel(String role) {
        return switch (role) {
            case "ADMIN" -> "Administrador";
            case "OWNER" -> "Proprietário";
            default      -> "Membro";
        };
    }
}