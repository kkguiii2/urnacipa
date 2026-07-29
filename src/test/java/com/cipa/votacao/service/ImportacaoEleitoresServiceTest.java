package com.cipa.votacao.service;

import com.cipa.votacao.config.ImportacaoEleitoresProperties;
import com.cipa.votacao.dto.importacao.DetalheImportacaoEleitorDto;
import com.cipa.votacao.dto.importacao.ResultadoImportacaoEleitoresDto;
import com.cipa.votacao.dto.importacao.StatusImportacaoEleitor;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.exception.PlanilhaImportacaoException;
import com.cipa.votacao.repository.UsuarioRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacaoEleitoresServiceTest {

    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PersistenciaEleitorService persistenciaEleitorService;

    private ImportacaoEleitoresProperties properties;
    private ImportacaoEleitoresService service;

    @BeforeEach
    void setUp() {
        properties = new ImportacaoEleitoresProperties();
        properties.setMaxFileSizeBytes(5 * 1024 * 1024);
        properties.setMaxDataRows(5_000);
        service = new ImportacaoEleitoresService(
                usuarioRepository,
                persistenciaEleitorService,
                new LeitorPlanilhaEleitores(properties));
    }

    @Test
    void importaArquivoValidoComVariasLinhas() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "Joao da Silva");
            row(sheet, 2, "67890", "Maria Oliveira");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.totalLinhas()).isEqualTo(2);
        assertThat(result.importados()).isEqualTo(2);
        assertThat(result.detalhes()).extracting(DetalheImportacaoEleitorDto::status)
                .containsExactly(StatusImportacaoEleitor.IMPORTADO, StatusImportacaoEleitor.IMPORTADO);
        verify(persistenciaEleitorService, times(2)).salvarNovo(any(Usuario.class));
    }

    @Test
    void preservaZerosAEsquerdaEmCelulaTextual() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "00123", "Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.detalhes().get(0).matricula()).isEqualTo("00123");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(persistenciaEleitorService).salvarNovo(captor.capture());
        assertThat(captor.getValue().getMatricula()).isEqualTo("00123");
    }

    @Test
    void localizaCabecalhosEmOrdemDiferente() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "nome", "matricula");
            row(sheet, 1, "Maria Oliveira", "67890");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.detalhes().get(0).matricula()).isEqualTo("67890");
        assertThat(result.detalhes().get(0).nome()).isEqualTo("Maria Oliveira");
    }

    @Test
    void normalizaEspacosECaixaDosCabecalhos() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "  MATRICULA  ", " Nome ");
            row(sheet, 1, "12345", "Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.importados()).isEqualTo(1);
    }

    @Test
    void ignoraLinhaCompletamenteVazia() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            sheet.createRow(1);
            row(sheet, 2, "12345", "Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.totalLinhas()).isEqualTo(1);
        assertThat(result.importados()).isEqualTo(1);
    }

    @Test
    void rejeitaMatriculaVazia() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "  ", "Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertInvalid(result, "Matrícula não informada.");
    }

    @Test
    void rejeitaNomeVazio() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "   ");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertInvalid(result, "Nome não informado.");
    }

    @Test
    void marcaDuplicidadeDentroDaPlanilha() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "Joao");
            row(sheet, 2, "12345", "Joao repetido");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.importados()).isEqualTo(1);
        assertThat(result.duplicadosNaPlanilha()).isEqualTo(1);
        assertThat(result.detalhes().get(1).status()).isEqualTo(StatusImportacaoEleitor.DUPLICADO_NA_PLANILHA);
        assertThat(result.detalhes().get(1).motivo()).isEqualTo("Matrícula repetida na linha 2.");
    }

    @Test
    void ignoraMatriculaJaExistenteSemAlterarUsuario() throws Exception {
        Usuario existente = usuario("12345", "Nome existente");
        when(usuarioRepository.findByMatriculaIn(anyCollection())).thenReturn(List.of(existente));
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "Nome novo");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.jaCadastrados()).isEqualTo(1);
        assertThat(result.detalhes().get(0).status()).isEqualTo(StatusImportacaoEleitor.JA_CADASTRADO);
        assertThat(existente.getNome()).isEqualTo("Nome existente");
        verify(persistenciaEleitorService, never()).salvarNovo(any());
    }

    @Test
    void rejeitaNomeAcimaDoLimiteDoBanco() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "A".repeat(256));
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertInvalid(result, "Nome ultrapassa o limite permitido de 255 caracteres.");
    }

    @Test
    void leMatriculaNumericaComDataFormatter() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(12345);
            row.createCell(1).setCellValue("Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.detalhes().get(0).matricula()).isEqualTo("12345");
        assertThat(result.importados()).isEqualTo(1);
    }

    @Test
    void preservaFormatoComZerosEmMatriculaNumerica() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            Row row = sheet.createRow(1);
            CellStyle style = sheet.getWorkbook().createCellStyle();
            style.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("00000"));
            row.createCell(0).setCellValue(123);
            row.getCell(0).setCellStyle(style);
            row.createCell(1).setCellValue("Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.detalhes().get(0).matricula()).isEqualTo("00123");
    }

    @Test
    void leMatriculaEmCelulaTextual() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "98765432109876543210", "Maria");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.detalhes().get(0).matricula()).isEqualTo("98765432109876543210");
        assertThat(result.importados()).isEqualTo(1);
    }

    @Test
    void interrompeQuandoCabecalhoObrigatorioNaoExiste() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "codigo", "nome");
            row(sheet, 1, "12345", "Joao");
        });

        assertThatThrownBy(() -> service.importar(file))
                .isInstanceOf(PlanilhaImportacaoException.class)
                .hasMessage("A coluna 'matricula' não foi encontrada.");
        verify(persistenciaEleitorService, never()).salvarNovo(any());
    }

    @Test
    void rejeitaArquivoVazio() {
        MockMultipartFile file = new MockMultipartFile("arquivo", "eleitores.xlsx", XLSX_TYPE, new byte[0]);

        assertThatThrownBy(() -> service.importar(file))
                .isInstanceOf(PlanilhaImportacaoException.class)
                .hasMessage("O arquivo está vazio.");
    }

    @Test
    void rejeitaArquivoCorrompido() {
        MockMultipartFile file = new MockMultipartFile("arquivo", "eleitores.xlsx", XLSX_TYPE, "nao-e-xlsx".getBytes());

        assertThatThrownBy(() -> service.importar(file))
                .isInstanceOf(PlanilhaImportacaoException.class)
                .hasMessage("Não foi possível ler a planilha.");
    }

    @Test
    void rejeitaExtensaoNaoPermitidaMesmoComConteudoXlsx() throws Exception {
        MockMultipartFile xlsx = workbook("eleitores.xls", sheet -> header(sheet, "matricula", "nome"));

        assertThatThrownBy(() -> service.importar(xlsx))
                .isInstanceOf(PlanilhaImportacaoException.class)
                .hasMessage("O formato enviado não é suportado. Envie um arquivo .xlsx.");
    }

    @Test
    void importaLinhasValidasQuandoArquivoTambemPossuiLinhaInvalida() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "Joao");
            row(sheet, 2, "", "Maria");
            row(sheet, 3, "67890", "Ana");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.totalLinhas()).isEqualTo(3);
        assertThat(result.importados()).isEqualTo(2);
        assertThat(result.rejeitados()).isEqualTo(1);
    }

    @Test
    void trataViolacaoDeUnicidadeConcorrenteSemInterromperDemaisLinhas() throws Exception {
        doThrow(new DataIntegrityViolationException("unique", new SQLException("unique", "23505")))
                .doNothing()
                .when(persistenciaEleitorService).salvarNovo(any(Usuario.class));
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "Joao");
            row(sheet, 2, "67890", "Maria");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertThat(result.jaCadastrados()).isEqualTo(1);
        assertThat(result.importados()).isEqualTo(1);
        assertThat(result.detalhes()).extracting(DetalheImportacaoEleitorDto::status)
                .containsExactly(StatusImportacaoEleitor.JA_CADASTRADO, StatusImportacaoEleitor.IMPORTADO);
    }

    @Test
    void rejeitaFormulaSemAvaliarConteudo() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellFormula("1+1");
            row.createCell(1).setCellValue("Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertInvalid(result, "Células com fórmula não são permitidas.");
    }

    @Test
    void rejeitaMatriculaForaDoFormatoAtual() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "ABC-123", "Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertInvalid(result, "Formato de matrícula inválido.");
    }

    @Test
    void rejeitaMatriculaAcimaDeVinteCaracteres() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "1".repeat(21), "Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertInvalid(result, "Matrícula ultrapassa o limite permitido de 20 caracteres.");
    }

    @Test
    void rejeitaMatriculaNumericaQuePodeTerSidoArredondadaPeloExcel() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(12_345_678_901_234_567d);
            row.createCell(1).setCellValue("Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertInvalid(
                result,
                "Matrícula numérica não pode exceder 15 dígitos. Formate a coluna como Texto.");
    }

    @Test
    void rejeitaMatriculaNumericaFracionariaMesmoQuandoFormatoOcultaDecimais() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            Row row = sheet.createRow(1);
            CellStyle style = sheet.getWorkbook().createCellStyle();
            style.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("0"));
            row.createCell(0).setCellValue(123.4);
            row.getCell(0).setCellStyle(style);
            row.createCell(1).setCellValue("Joao");
        });

        ResultadoImportacaoEleitoresDto result = service.importar(file);

        assertInvalid(result, "Matrícula numérica deve representar um número inteiro.");
    }

    @Test
    void rejeitaConteudoCompactadoAcimaDoLimiteAntesDeAbrirWorkbook() throws Exception {
        properties.setMaxExpandedSizeBytes(1_024);
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "A".repeat(20_000));
        });

        assertThatThrownBy(() -> service.importar(file))
                .isInstanceOf(PlanilhaImportacaoException.class)
                .hasMessage("O conteúdo descompactado da planilha ultrapassa o limite permitido.");
        verify(persistenciaEleitorService, never()).salvarNovo(any());
    }

    @Test
    void rejeitaPacoteComProjetoVbaMesmoUsandoExtensaoXlsx() throws Exception {
        MockMultipartFile original = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "Joao");
        });
        MockMultipartFile file = adicionarEntradaZip(original, "xl/vbaProject.bin", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.importar(file))
                .isInstanceOf(PlanilhaImportacaoException.class)
                .hasMessage("Planilhas com macros não são permitidas.");
    }

    @Test
    void rejeitaArquivoAcimaDoLimiteAntesDeLer() {
        properties.setMaxFileSizeBytes(8);
        MockMultipartFile file = new MockMultipartFile("arquivo", "eleitores.xlsx", XLSX_TYPE, new byte[9]);

        assertThatThrownBy(() -> service.importar(file))
                .isInstanceOf(PlanilhaImportacaoException.class)
                .hasMessage("O arquivo ultrapassa o limite permitido de 8 bytes.");
    }

    @Test
    void rejeitaQuantidadeDeLinhasAcimaDoLimiteSemPersistirParcialmente() throws Exception {
        properties.setMaxDataRows(1);
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "Joao");
            row(sheet, 2, "67890", "Maria");
        });

        assertThatThrownBy(() -> service.importar(file))
                .isInstanceOf(PlanilhaImportacaoException.class)
                .hasMessage("A planilha ultrapassa o limite permitido de 1 linhas de dados.");
        verify(persistenciaEleitorService, never()).salvarNovo(any());
    }

    @Test
    void consultaMatriculasExistentesUmaUnicaVez() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, "12345", "Joao");
            row(sheet, 2, "67890", "Maria");
        });

        service.importar(file);

        verify(usuarioRepository).findByMatriculaIn(anyCollection());
    }

    @Test
    void criaUsuarioComIdNuloAtivoENaoVotante() throws Exception {
        MockMultipartFile file = workbook("eleitores.xlsx", sheet -> {
            header(sheet, "matricula", "nome");
            row(sheet, 1, " 12345 ", " Joao da Silva ");
        });

        service.importar(file);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(persistenciaEleitorService).salvarNovo(captor.capture());
        Usuario usuario = captor.getValue();
        assertThat(usuario.getId()).isNull();
        assertThat(usuario.getMatricula()).isEqualTo("12345");
        assertThat(usuario.getNome()).isEqualTo("Joao da Silva");
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.isVotou()).isFalse();
    }

    private void assertInvalid(ResultadoImportacaoEleitoresDto result, String reason) {
        assertThat(result.importados()).isZero();
        assertThat(result.rejeitados()).isEqualTo(1);
        assertThat(result.detalhes().get(0).status()).isEqualTo(StatusImportacaoEleitor.INVALIDO);
        assertThat(result.detalhes().get(0).motivo()).isEqualTo(reason);
    }

    private static Usuario usuario(String matricula, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setMatricula(matricula);
        usuario.setNome(nome);
        usuario.setAtivo(true);
        return usuario;
    }

    private static MockMultipartFile workbook(String filename, Consumer<Sheet> content) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Eleitores");
            content.accept(sheet);
            workbook.write(output);
            return new MockMultipartFile("arquivo", filename, XLSX_TYPE, output.toByteArray());
        }
    }

    private static MockMultipartFile adicionarEntradaZip(
            MockMultipartFile original,
            String entryName,
            byte[] content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(original.getBytes()));
             ZipOutputStream zip = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                zip.putNextEntry(new ZipEntry(entry.getName()));
                input.transferTo(zip);
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content);
            zip.closeEntry();
        }
        return new MockMultipartFile("arquivo", "eleitores.xlsx", XLSX_TYPE, output.toByteArray());
    }

    private static void header(Sheet sheet, String... values) {
        row(sheet, 0, values);
    }

    private static void row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
