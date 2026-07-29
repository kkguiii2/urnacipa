package com.cipa.votacao.service;

import com.cipa.votacao.config.ImportacaoEleitoresProperties;
import com.cipa.votacao.dto.importacao.LinhaImportacaoEleitorDto;
import com.cipa.votacao.dto.importacao.PlanilhaEleitoresDto;
import com.cipa.votacao.exception.PlanilhaImportacaoException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Valida limites e estrutura do pacote OOXML antes de ler a primeira aba da
 * planilha de eleitores.
 */
@Component
@RequiredArgsConstructor
public class LeitorPlanilhaEleitores {

    private static final String MATRICULA = "matricula";
    private static final String NOME = "nome";
    private static final int ZIP_SIGNATURE_LENGTH = 4;
    private static final BigDecimal MAX_SAFE_NUMERIC_MATRICULA = new BigDecimal("999999999999999");

    private final ImportacaoEleitoresProperties properties;

    /**
     * Lê as colunas obrigatórias {@code matricula} e {@code nome}, preservando a
     * representação exibida das células e registrando erros próprios da linha.
     */
    public PlanilhaEleitoresDto ler(MultipartFile arquivo) {
        validarArquivo(arquivo);
        validarPacoteCompactado(arquivo);
        String nomeArquivo = nomeSeguro(arquivo.getOriginalFilename());

        try (InputStream input = arquivo.getInputStream(); OPCPackage pacote = OPCPackage.open(input)) {
            if (pacote.getPartsByContentType(XSSFRelation.WORKBOOK.getContentType()).isEmpty()) {
                throw new PlanilhaImportacaoException("O formato enviado não é suportado. Envie um arquivo .xlsx.");
            }
            try (XSSFWorkbook workbook = new XSSFWorkbook(pacote)) {
                return lerWorkbook(workbook, nomeArquivo);
            }
        } catch (PlanilhaImportacaoException e) {
            throw e;
        } catch (Exception e) {
            throw new PlanilhaImportacaoException("Não foi possível ler a planilha.");
        }
    }

    private PlanilhaEleitoresDto lerWorkbook(XSSFWorkbook workbook, String nomeArquivo) {
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        if (workbook.getNumberOfSheets() == 0) {
            throw new PlanilhaImportacaoException("O arquivo está vazio.");
        }

        Sheet sheet = workbook.getSheetAt(0);
        Row header = sheet.getRow(0);
        if (header == null || linhaVazia(header, formatter)) {
            throw new PlanilhaImportacaoException("O arquivo está vazio.");
        }

        Map<String, Integer> columns = mapearCabecalhos(header, formatter);
        validarCabecalho(columns, MATRICULA);
        validarCabecalho(columns, NOME);

        List<LinhaImportacaoEleitorDto> linhas = new ArrayList<>();
        for (Row row : sheet) {
            if (row.getRowNum() == 0 || linhaVazia(row, formatter)) {
                continue;
            }
            if (linhas.size() >= properties.getMaxDataRows()) {
                throw new PlanilhaImportacaoException(
                        "A planilha ultrapassa o limite permitido de " + properties.getMaxDataRows() + " linhas de dados.");
            }
            Cell matriculaCell = row.getCell(columns.get(MATRICULA), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            Cell nomeCell = row.getCell(columns.get(NOME), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            boolean formula = formula(matriculaCell) || formula(nomeCell);
            String erroLeitura = formula
                    ? "Células com fórmula não são permitidas."
                    : validarMatriculaNumerica(matriculaCell);
            linhas.add(new LinhaImportacaoEleitorDto(
                    row.getRowNum() + 1,
                    formula(matriculaCell) ? "" : valor(matriculaCell, formatter),
                    formula(nomeCell) ? "" : valor(nomeCell, formatter),
                    erroLeitura));
        }

        return new PlanilhaEleitoresDto(nomeArquivo, linhas);
    }

    private Map<String, Integer> mapearCabecalhos(Row row, DataFormatter formatter) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < row.getLastCellNum(); index++) {
            Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (formula(cell)) {
                throw new PlanilhaImportacaoException("Cabeçalhos com fórmula não são permitidos.");
            }
            String header = valor(cell, formatter).toLowerCase(Locale.ROOT);
            if (!header.isEmpty() && columns.putIfAbsent(header, index) != null) {
                throw new PlanilhaImportacaoException("O cabeçalho '" + header + "' está duplicado.");
            }
        }
        return columns;
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new PlanilhaImportacaoException("O arquivo está vazio.");
        }
        String nome = nomeSeguro(arquivo.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!nome.endsWith(".xlsx")) {
            throw new PlanilhaImportacaoException("O formato enviado não é suportado. Envie um arquivo .xlsx.");
        }
        if (arquivo.getSize() > properties.getMaxFileSizeBytes()) {
            throw new PlanilhaImportacaoException(
                    "O arquivo ultrapassa o limite permitido de " + properties.getMaxFileSizeBytes() + " bytes.");
        }
    }

    private void validarPacoteCompactado(MultipartFile arquivo) {
        try (InputStream input = arquivo.getInputStream();
             PushbackInputStream seguro = new PushbackInputStream(
                     new BufferedInputStream(input), ZIP_SIGNATURE_LENGTH)) {
            validarAssinaturaZip(seguro);
            try (ZipInputStream zip = new ZipInputStream(seguro)) {
                byte[] buffer = new byte[8_192];
                long expandedBytes = 0;
                int entries = 0;
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entries++;
                    if (entries > properties.getMaxZipEntries()) {
                        throw new PlanilhaImportacaoException("A planilha possui arquivos internos demais.");
                    }
                    if (entry.getName().toLowerCase(Locale.ROOT).endsWith("vbaproject.bin")) {
                        throw new PlanilhaImportacaoException("Planilhas com macros não são permitidas.");
                    }
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        expandedBytes += read;
                        if (expandedBytes > properties.getMaxExpandedSizeBytes()) {
                            throw new PlanilhaImportacaoException(
                                    "O conteúdo descompactado da planilha ultrapassa o limite permitido.");
                        }
                    }
                    zip.closeEntry();
                }
                if (entries == 0) {
                    throw new PlanilhaImportacaoException("Não foi possível ler a planilha.");
                }
            }
        } catch (PlanilhaImportacaoException e) {
            throw e;
        } catch (IOException e) {
            throw new PlanilhaImportacaoException("Não foi possível ler a planilha.");
        }
    }

    private void validarAssinaturaZip(PushbackInputStream input) throws IOException {
        byte[] signature = input.readNBytes(ZIP_SIGNATURE_LENGTH);
        if (signature.length < ZIP_SIGNATURE_LENGTH
                || signature[0] != 0x50
                || signature[1] != 0x4B
                || signature[2] != 0x03
                || signature[3] != 0x04) {
            throw new PlanilhaImportacaoException("Não foi possível ler a planilha.");
        }
        input.unread(signature);
    }

    private void validarCabecalho(Map<String, Integer> columns, String required) {
        if (!columns.containsKey(required)) {
            throw new PlanilhaImportacaoException("A coluna '" + required + "' não foi encontrada.");
        }
    }

    private String validarMatriculaNumerica(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            return null;
        }
        BigDecimal numericValue = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
        if (numericValue.scale() > 0) {
            return "Matrícula numérica deve representar um número inteiro.";
        }
        if (numericValue.abs().compareTo(MAX_SAFE_NUMERIC_MATRICULA) > 0) {
            return "Matrícula numérica não pode exceder 15 dígitos. Formate a coluna como Texto.";
        }
        return null;
    }

    private boolean linhaVazia(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (formula(cell) || !valor(cell, formatter).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String valor(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private boolean formula(Cell cell) {
        return cell != null && cell.getCellType() == CellType.FORMULA;
    }

    private String nomeSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "arquivo.xlsx";
        }
        String normalized = original.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }
}
