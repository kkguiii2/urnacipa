package com.cipa.votacao.service;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.entity.ConfiguracaoEleicao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.destinatario:}")
    private String emailDestinatario;

    public void enviarRelatorio(byte[] relatorio, byte[] pdf) {
        if (emailDestinatario == null || emailDestinatario.isEmpty()) {
            log.warn("E-mail do destinatário não configurado. Relatório não será enviado.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailDestinatario);
            helper.setSubject("Relatório da Eleição CIPA - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            helper.setText("Prezado(a),\n\nSegue em anexo o relatório da eleição da CIPA.\n\nAtenciosamente,\nSistema de Votação CIPA");

            if (relatorio != null && relatorio.length > 0) {
                helper.addAttachment("relatorio_cipa.xlsx", new org.springframework.core.io.ByteArrayResource(relatorio),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }
            if (pdf != null && pdf.length > 0) {
                helper.addAttachment("relatorio_cipa.pdf", new org.springframework.core.io.ByteArrayResource(pdf));
            }

            mailSender.send(message);
            log.info("Relatório enviado com sucesso para {}", emailDestinatario);
        } catch (MessagingException e) {
            log.error("Erro ao enviar e-mail: {}", e.getMessage());
            throw new RuntimeException("Falha ao enviar e-mail: " + e.getMessage(), e);
        }
    }

    public void enviarRelatorioSimples(String conteudo, String nomeArquivo) {
        if (emailDestinatario == null || emailDestinatario.isEmpty()) {
            log.warn("E-mail do destinatário não configurado.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailDestinatario);
            message.setSubject("Relatório da Eleição CIPA - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            message.setText("Prezado(a),\n\nSegue o relatório da eleição da CIPA.\n\n" + conteudo + "\n\nAtenciosamente,\nSistema de Votação CIPA");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail: {}", e.getMessage());
        }
    }
}