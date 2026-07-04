package com.financeflow.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    
    public void sendPasswordResetEmail(String to, String token, String userName) {
        String resetUrl = emailProperties.getFrontendUrl() + "/reset-password?token=" + token;
        
        String subject = "Recuperação de Senha - FinanceFlow";
        String body = buildPasswordResetEmailBody(userName, resetUrl);
        
        sendHtmlEmail(to, subject, body);
    }
    
    public void sendEmailVerificationEmail(String to, String token, String userName) {
        String verificationUrl = emailProperties.getFrontendUrl() + "/verify-email?token=" + token;
        
        String subject = "Verifique seu e-mail - FinanceFlow";
        String body = buildEmailVerificationBody(userName, verificationUrl);
        
        sendHtmlEmail(to, subject, body);
    }

    /**
     * Envia notificação financeira por e-mail (orçamento excedido, saldo baixo, etc.).
     */
    public void sendNotificationEmail(String to, String userName, String title, String message) {
        String subject = title + " - FinanceFlow";
        String body = buildNotificationEmailBody(userName, title, message);
        sendHtmlEmail(to, subject, body);
    }
    
    private void sendHtmlEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(emailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            
            mailSender.send(message);
            
            log.info("Email sent successfully", Map.of("to", to, "subject", subject));
        } catch (MessagingException e) {
            log.error("Error sending email", Map.of("to", to, "error", e.getMessage()), e);
            throw new RuntimeException("Erro ao enviar e-mail: " + e.getMessage(), e);
        } catch (MailException e) {
            log.error("Mail service error", Map.of("to", to, "error", e.getMessage()), e);
            throw new RuntimeException("Erro no serviço de e-mail: " + e.getMessage(), e);
        }
    }
    
    private String buildPasswordResetEmailBody(String userName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4F46E5; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>FinanceFlow</h1>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                        <p>Clique no botão abaixo para criar uma nova senha:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Redefinir Senha</a>
                        </p>
                        <p>Ou copie e cole o link abaixo no seu navegador:</p>
                        <p style="word-break: break-all; color: #4F46E5;">%s</p>
                        <p><strong>Este link expira em 24 horas.</strong></p>
                        <p>Se você não solicitou esta alteração, ignore este e-mail.</p>
                    </div>
                    <div class="footer">
                        <p>Este é um e-mail automático, por favor não responda.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetUrl, resetUrl);
    }
    
    private String buildEmailVerificationBody(String userName, String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4F46E5; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>FinanceFlow</h1>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <p>Bem-vindo ao FinanceFlow!</p>
                        <p>Para completar seu cadastro, por favor verifique seu endereço de e-mail clicando no botão abaixo:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Verificar E-mail</a>
                        </p>
                        <p>Ou copie e cole o link abaixo no seu navegador:</p>
                        <p style="word-break: break-all; color: #4F46E5;">%s</p>
                        <p><strong>Este link expira em 72 horas.</strong></p>
                        <p>Se você não criou uma conta, ignore este e-mail.</p>
                    </div>
                    <div class="footer">
                        <p>Este é um e-mail automático, por favor não responda.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, verificationUrl, verificationUrl);
    }

    private String buildNotificationEmailBody(String userName, String title, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .alert-box { background-color: #FEF3C7; border-left: 4px solid #F59E0B; padding: 12px; margin: 16px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>FinanceFlow</h1>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <div class="alert-box">
                            <strong>%s</strong>
                            <p style="margin: 8px 0 0;">%s</p>
                        </div>
                        <p>Acesse o FinanceFlow para mais detalhes.</p>
                    </div>
                    <div class="footer">
                        <p>Este é um e-mail automático, por favor não responda.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, title, message.replace("\n", "<br>"));
    }
}
