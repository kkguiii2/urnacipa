package com.cipa.votacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class UploadServiceTest {

    @TempDir Path tempDir;

    @Test
    void validaERegravaImagemComNomeAleatorio() throws Exception {
        UploadService service = new UploadService();
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        MockMultipartFile file = new MockMultipartFile(
                "foto", "ataque.html", "text/html", bytes.toByteArray());

        String filename = service.salvarImagem(file);

        assertThat(filename).matches("[a-f0-9-]{36}\\.png");
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
        assertThat(ImageIO.read(tempDir.resolve(filename).toFile())).isNotNull();
    }

    @Test
    void rejeitaArquivoQueNaoForImagemMesmoComExtensaoPermitida() {
        UploadService service = new UploadService();
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "foto", "falsa.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> service.salvarImagem(file))
                .isInstanceOf(java.io.IOException.class);
        assertThat(tempDir).isEmptyDirectory();
    }
}
