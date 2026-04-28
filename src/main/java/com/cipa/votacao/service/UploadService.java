package com.cipa.votacao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    public String salvarImagem(MultipartFile arquivo) throws IOException {
        Path caminhoUpload = Paths.get(uploadPath).toAbsolutePath();
        if (!Files.exists(caminhoUpload)) {
            Files.createDirectories(caminhoUpload);
        }

        String nomeOriginal = arquivo.getOriginalFilename();
        String extensao = ".jpg";
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
        }
        String nomeArquivo = UUID.randomUUID().toString() + extensao;

        Path caminhoCompleto = caminhoUpload.resolve(nomeArquivo);
        Files.copy(arquivo.getInputStream(), caminhoCompleto, StandardCopyOption.REPLACE_EXISTING);

        // Return just the filename — the URL will be built by the templates/JS
        log.info("Imagem salva em: {}", caminhoCompleto);
        return nomeArquivo;
    }

    public boolean excluirImagem(String nomeArquivo) {
        try {
            Path path = Paths.get(uploadPath).toAbsolutePath().resolve(nomeArquivo);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Erro ao excluir imagem: {}", e.getMessage());
            return false;
        }
    }
}