package com.cipa.votacao.service;

import com.cipa.votacao.dto.importacao.DetalheImportacaoEleitorDto;
import com.cipa.votacao.dto.importacao.ResultadoImportacaoEleitoresDto;
import com.cipa.votacao.dto.importacao.StatusImportacaoEleitor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanilhaEleitoresServiceTest {

    private final PlanilhaEleitoresService service = new PlanilhaEleitoresService();

    @Test
    void geraModeloXlsxComCabecalhosExatosEExemplosRemoviveis() throws Exception {
        byte[] bytes = service.gerarModelo();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("matricula");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("nome");
            assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(2);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).contains("EXEMPLO");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).contains("EXEMPLO");
            assertThat(todasAsCelulas(sheet)).noneMatch(cell -> cell.getCellType() == CellType.FORMULA);
        }
    }

    @Test
    void geraRelatorioSomenteComLinhasNaoImportadas() throws Exception {
        ResultadoImportacaoEleitoresDto resultado = ResultadoImportacaoEleitoresDto.criar(
                "eleitores.xlsx",
                25,
                List.of(
                        detalhe(2, "12345", "Joao", StatusImportacaoEleitor.IMPORTADO, "Usuário cadastrado com sucesso."),
                        detalhe(3, "", "Maria", StatusImportacaoEleitor.INVALIDO, "Matrícula não informada."),
                        detalhe(4, "12345", "Joao 2", StatusImportacaoEleitor.DUPLICADO_NA_PLANILHA, "Matrícula repetida na linha 2.")));

        byte[] bytes = service.gerarRelatorioErros(resultado);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("linha_original");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("matricula");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("nome");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("status");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("motivo");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(3);
            assertThat(sheet.getRow(2).getCell(0).getNumericCellValue()).isEqualTo(4);
            DataFormatter formatter = new DataFormatter();
            assertThat(todasAsCelulas(sheet)).noneMatch(cell -> formatter.formatCellValue(cell).equals("Joao"));
        }
    }

    private static DetalheImportacaoEleitorDto detalhe(
            int linha,
            String matricula,
            String nome,
            StatusImportacaoEleitor status,
            String motivo) {
        return new DetalheImportacaoEleitorDto(linha, matricula, nome, status, motivo);
    }

    private static List<org.apache.poi.ss.usermodel.Cell> todasAsCelulas(Sheet sheet) {
        return java.util.stream.StreamSupport.stream(sheet.spliterator(), false)
                .flatMap(row -> java.util.stream.StreamSupport.stream(row.spliterator(), false))
                .toList();
    }
}
