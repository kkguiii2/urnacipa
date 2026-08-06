package com.cipa.votacao.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class UploadService {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 16_000_000L;
    private static final Set<String> FORMATOS_PERMITIDOS = Set.of("jpeg", "jpg", "png");

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    public String salvarImagem(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty() || arquivo.getSize() > MAX_IMAGE_BYTES) {
            throw new IOException("Arquivo de imagem ausente ou acima de 5 MB.");
        }

        ImagemValidada imagem = decodificarImagem(arquivo);
        Path diretorio = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(diretorio);

        String extensao = "png".equals(imagem.formato()) ? ".png" : ".jpg";
        String nomeArquivo = UUID.randomUUID() + extensao;
        Path destino = diretorio.resolve(nomeArquivo).normalize();
        if (!destino.getParent().equals(diretorio)) {
            throw new IOException("Destino de upload inválido.");
        }

        try (OutputStream output = Files.newOutputStream(
                destino,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            String formatoSaida = "png".equals(imagem.formato()) ? "png" : "jpg";
            if (!ImageIO.write(imagem.conteudo(), formatoSaida, output)) {
                throw new IOException("Não foi possível regravar a imagem.");
            }
        } catch (IOException e) {
            Files.deleteIfExists(destino);
            throw e;
        }

        log.info("Imagem de candidato validada e armazenada com nome aleatório.");
        return nomeArquivo;
    }

    private ImagemValidada decodificarImagem(MultipartFile arquivo) throws IOException {
        try (InputStream input = arquivo.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new IOException("Conteúdo não é uma imagem válida.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IOException("Formato de imagem não reconhecido.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String formato = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!FORMATOS_PERMITIDOS.contains(formato)) {
                    throw new IOException("Use somente imagem JPEG ou PNG.");
                }
                int largura = reader.getWidth(0);
                int altura = reader.getHeight(0);
                if (largura <= 0 || altura <= 0
                        || largura > MAX_DIMENSION || altura > MAX_DIMENSION
                        || (long) largura * altura > MAX_PIXELS) {
                    throw new IOException("Dimensões da imagem não são permitidas.");
                }
                BufferedImage conteudo = reader.read(0);
                if (conteudo == null) {
                    throw new IOException("Imagem inválida.");
                }
                return new ImagemValidada("jpg".equals(formato) ? "jpeg" : formato, conteudo);
            } finally {
                reader.dispose();
            }
        }
    }

    public boolean excluirImagem(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.matches("^[a-f0-9-]{36}\\.(jpg|jpeg|png)$")) {
            return false;
        }
        try {
            Path diretorio = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path destino = diretorio.resolve(nomeArquivo).normalize();
            if (!destino.getParent().equals(diretorio)) {
                return false;
            }
            return Files.deleteIfExists(destino);
        } catch (IOException e) {
            log.error("Erro ao excluir imagem de candidato.");
            return false;
        }
    }

    private record ImagemValidada(String formato, BufferedImage conteudo) {
    }
}
