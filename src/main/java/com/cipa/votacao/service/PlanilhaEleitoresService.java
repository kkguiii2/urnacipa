package com.cipa.votacao.service;

import com.cipa.votacao.dto.importacao.DetalheImportacaoEleitorDto;
import com.cipa.votacao.dto.importacao.ResultadoImportacaoEleitoresDto;
import com.cipa.votacao.dto.importacao.StatusImportacaoEleitor;
import com.cipa.votacao.exception.PlanilhaImportacaoException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PlanilhaEleitoresService {

    public byte[] gerarModelo() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Eleitores");
            criarCabecalho(sheet, workbook, "matricula", "nome");
            criarLinha(sheet, 1, "00001", "João da Silva - EXEMPLO (REMOVER OU SUBSTITUIR)");
            criarLinha(sheet, 2, "00002", "Maria Oliveira - EXEMPLO (REMOVER OU SUBSTITUIR)");
            configurarColunas(sheet);
            sheet.createFreezePane(0, 1);
            return escrever(workbook);
        } catch (IOException e) {
            throw new PlanilhaImportacaoException("Não foi possível gerar a planilha-modelo.");
        }
    }

    public byte[] gerarRelatorioErros(ResultadoImportacaoEleitoresDto resultado) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Linhas não importadas");
            criarCabecalho(sheet, workbook, "linha_original", "matricula", "nome", "status", "motivo");
            int rowIndex = 1;
            for (DetalheImportacaoEleitorDto detalhe : resultado.detalhes()) {
                if (detalhe.status() == StatusImportacaoEleitor.IMPORTADO) {
                    continue;
                }
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(detalhe.linha());
                row.createCell(1).setCellValue(detalhe.matricula());
                row.createCell(2).setCellValue(detalhe.nome());
                row.createCell(3).setCellValue(detalhe.status().name());
                row.createCell(4).setCellValue(detalhe.motivo());
            }
            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 24 * 256);
            sheet.setColumnWidth(2, 36 * 256);
            sheet.setColumnWidth(3, 28 * 256);
            sheet.setColumnWidth(4, 54 * 256);
            sheet.createFreezePane(0, 1);
            return escrever(workbook);
        } catch (IOException e) {
            throw new PlanilhaImportacaoException("Não foi possível gerar o relatório de erros.");
        }
    }

    private void criarCabecalho(Sheet sheet, XSSFWorkbook workbook, String... headers) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row row = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(style);
        }
    }

    private void criarLinha(Sheet sheet, int index, String matricula, String nome) {
        Row row = sheet.createRow(index);
        row.createCell(0).setCellValue(matricula);
        row.createCell(1).setCellValue(nome);
    }

    private void configurarColunas(Sheet sheet) {
        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 60 * 256);
    }

    private byte[] escrever(XSSFWorkbook workbook) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
