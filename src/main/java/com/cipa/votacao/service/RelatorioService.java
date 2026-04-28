package com.cipa.votacao.service;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.entity.ConfiguracaoEleicao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelatorioService {

    private final VotacaoService votacaoService;
    private final CandidatoService candidatoService;
    private final ConfiguracaoService configuracaoService;
    private final UsuarioService usuarioService;
    private final EmailService emailService;

    // ==================== COLOR PALETTE ====================
    // Dark navy header
    private static final byte[] COLOR_HEADER = {(byte) 30, (byte) 58, (byte) 95};
    // Green accent for highlights
    private static final byte[] COLOR_GREEN = {(byte) 46, (byte) 204, (byte) 113};
    // Light blue zebra row
    private static final byte[] COLOR_ZEBRA = {(byte) 235, (byte) 245, (byte) 255};
    // Gold for 1st place
    private static final byte[] COLOR_GOLD = {(byte) 255, (byte) 215, (byte) 0};
    // Silver for 2nd
    private static final byte[] COLOR_SILVER = {(byte) 210, (byte) 210, (byte) 210};
    // Bronze for 3rd
    private static final byte[] COLOR_BRONZE = {(byte) 205, (byte) 127, (byte) 50};
    // Summary section bg
    private static final byte[] COLOR_SUMMARY_BG = {(byte) 240, (byte) 248, (byte) 255};
    // Summary label bg
    private static final byte[] COLOR_SUMMARY_LABEL = {(byte) 44, (byte) 62, (byte) 80};

    /**
     * Generates a professional Excel report (.xlsx)
     */
    public byte[] gerarRelatorioExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            // ── Sheet: Resultado da Eleição ──
            XSSFSheet sheet = workbook.createSheet("Resultado da Eleição");
            sheet.setDisplayGridlines(false);

            // Gather data
            List<Object[]> resultados = votacaoService.getResultados();
            long totalVotos = votacaoService.contarTotalVotos();
            long totalEleitores = usuarioService.contarTotalAtivos();
            ConfiguracaoEleicao config = configuracaoService.getConfiguracao();
            double participacao = totalEleitores > 0 ? (totalVotos * 100.0 / totalEleitores) : 0;

            // ── Styles ──
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subtitleStyle = createSubtitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dataStyleAlt = createDataStyleAlt(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle numberStyleAlt = createNumberStyleAlt(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            CellStyle percentStyleAlt = createPercentStyleAlt(workbook);
            CellStyle positionStyle = createPositionStyle(workbook);
            CellStyle positionStyleAlt = createPositionStyleAlt(workbook);
            CellStyle summaryLabelStyle = createSummaryLabelStyle(workbook);
            CellStyle summaryValueStyle = createSummaryValueStyle(workbook);
            CellStyle summaryPercentStyle = createSummaryPercentStyle(workbook);
            CellStyle dateInfoStyle = createDateInfoStyle(workbook);

            int rowIdx = 0;

            // ── TITLE SECTION ──
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.setHeightInPoints(45);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RELATÓRIO DA ELEIÇÃO CIPA");
            titleCell.setCellStyle(titleStyle);

            // Subtitle with date
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
            Row subtitleRow = sheet.createRow(rowIdx++);
            subtitleRow.setHeightInPoints(22);
            Cell subtitleCell = subtitleRow.createCell(0);
            subtitleCell.setCellValue("Gerado em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")));
            subtitleCell.setCellStyle(subtitleStyle);

            // Election period info
            if (config.getDataInicio() != null || config.getDataFim() != null) {
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
                Row dateRow = sheet.createRow(rowIdx++);
                dateRow.setHeightInPoints(20);
                Cell dateCell = dateRow.createCell(0);
                StringBuilder period = new StringBuilder("Período: ");
                if (config.getDataInicio() != null) {
                    period.append(config.getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                }
                period.append(" até ");
                if (config.getDataFim() != null) {
                    period.append(config.getDataFim().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                }
                dateCell.setCellValue(period.toString());
                dateCell.setCellStyle(dateInfoStyle);
            }

            // Blank row
            rowIdx++;

            // ── SUMMARY CARDS ──
            Row summaryHeaderRow = sheet.createRow(rowIdx++);
            summaryHeaderRow.setHeightInPoints(30);

            String[] summaryLabels = {"Total de Eleitores", "Votos Registrados", "Participação"};
            Object[] summaryValues = {totalEleitores, totalVotos, participacao};

            for (int i = 0; i < 3; i++) {
                int col = i * 2;
                Cell labelCell = summaryHeaderRow.createCell(col);
                labelCell.setCellValue(summaryLabels[i]);
                labelCell.setCellStyle(summaryLabelStyle);
            }

            Row summaryValueRow = sheet.createRow(rowIdx++);
            summaryValueRow.setHeightInPoints(35);

            for (int i = 0; i < 3; i++) {
                int col = i * 2;
                Cell valCell = summaryValueRow.createCell(col);
                if (i == 2) {
                    valCell.setCellValue((double) summaryValues[i] / 100.0);
                    valCell.setCellStyle(summaryPercentStyle);
                } else {
                    valCell.setCellValue(((Long) summaryValues[i]).doubleValue());
                    valCell.setCellStyle(summaryValueStyle);
                }
            }

            // Blank rows
            rowIdx++;
            rowIdx++;

            // ── RANKING TABLE HEADER ──
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
            Row sectionRow = sheet.createRow(rowIdx++);
            sectionRow.setHeightInPoints(28);
            Cell sectionCell = sectionRow.createCell(0);
            sectionCell.setCellValue("RANKING DE CANDIDATOS");
            sectionCell.setCellStyle(createSectionTitleStyle(workbook));

            rowIdx++; // small gap

            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(32);
            String[] headers = {"Posição", "Número", "Candidato", "Votos", "Percentual"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── DATA ROWS ──
            int posicao = 1;
            for (Object[] resultado : resultados) {
                Long candidatoId = (Long) resultado[0];
                Long votos = (Long) resultado[1];
                double percentual = totalVotos > 0 ? (votos * 100.0 / totalVotos) : 0;

                Candidato candidato = candidatoService.buscarPorId(candidatoId).orElse(null);
                if (candidato == null) {
                    posicao++;
                    continue;
                }

                boolean isAlt = (posicao % 2 == 0);
                Row dataRow = sheet.createRow(rowIdx++);
                dataRow.setHeightInPoints(28);

                // Position with medal emoji
                Cell posCell = dataRow.createCell(0);
                String posText = posicao + "º";
                if (posicao == 1) posText = "🥇 1º";
                else if (posicao == 2) posText = "🥈 2º";
                else if (posicao == 3) posText = "🥉 3º";
                posCell.setCellValue(posText);
                posCell.setCellStyle(isAlt ? positionStyleAlt : positionStyle);

                // Apply position-based highlight for top 3
                CellStyle rowDataStyle = isAlt ? dataStyleAlt : dataStyle;
                CellStyle rowNumberStyle = isAlt ? numberStyleAlt : numberStyle;
                CellStyle rowPercentStyle = isAlt ? percentStyleAlt : percentStyle;

                if (posicao <= 3) {
                    rowDataStyle = createMedalRowStyle(workbook, posicao, false);
                    rowNumberStyle = createMedalRowStyle(workbook, posicao, true);
                    rowPercentStyle = createMedalPercentStyle(workbook, posicao);
                }

                // Candidate number
                Cell numCell = dataRow.createCell(1);
                numCell.setCellValue(candidato.getNumero());
                numCell.setCellStyle(rowNumberStyle);

                // Candidate name
                Cell nameCell = dataRow.createCell(2);
                nameCell.setCellValue(candidato.getNome());
                nameCell.setCellStyle(rowDataStyle);

                // Votes
                Cell votesCell = dataRow.createCell(3);
                votesCell.setCellValue(votos);
                votesCell.setCellStyle(rowNumberStyle);

                // Percentage
                Cell pctCell = dataRow.createCell(4);
                pctCell.setCellValue(percentual / 100.0);
                pctCell.setCellStyle(rowPercentStyle);

                posicao++;
            }

            // ── TOTAL ROW ──
            rowIdx++; // gap
            Row totalRow = sheet.createRow(rowIdx++);
            totalRow.setHeightInPoints(30);

            Cell totalLabelCell = totalRow.createCell(2);
            totalLabelCell.setCellValue("TOTAL");
            totalLabelCell.setCellStyle(summaryLabelStyle);

            Cell totalVotosCell = totalRow.createCell(3);
            totalVotosCell.setCellValue(totalVotos);
            totalVotosCell.setCellStyle(summaryValueStyle);

            Cell totalPctCell = totalRow.createCell(4);
            totalPctCell.setCellValue(1.0);
            totalPctCell.setCellStyle(summaryPercentStyle);

            // ── AUTO-FIT COLUMNS ──
            sheet.setColumnWidth(0, 14 * 256);  // Posição
            sheet.setColumnWidth(1, 12 * 256);  // Número
            sheet.setColumnWidth(2, 35 * 256);  // Candidato
            sheet.setColumnWidth(3, 14 * 256);  // Votos
            sheet.setColumnWidth(4, 16 * 256);  // Percentual

            // ── FREEZE PANES (header visible while scrolling) ──
            sheet.createFreezePane(0, rowIdx - resultados.size());

            // ── PRINT SETUP ──
            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.setFitToPage(true);

            // Write to bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Erro ao gerar relatório Excel: {}", e.getMessage());
            return new byte[0];
        }
    }

    // ==================== STYLE FACTORY METHODS ====================

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 22);
        font.setBold(true);
        font.setColor(new XSSFColor(COLOR_HEADER, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubtitleStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setItalic(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 120, (byte) 120, (byte) 120}, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateInfoStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        font.setColor(new XSSFColor(new byte[]{(byte) 100, (byte) 100, (byte) 100}, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createSectionTitleStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 14);
        font.setBold(true);
        font.setColor(new XSSFColor(COLOR_HEADER, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBottomBorderColor(new XSSFColor(COLOR_GREEN, null).getIndex());
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(COLOR_HEADER, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(style, IndexedColors.WHITE.getIndex());
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(style, IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle createDataStyleAlt(XSSFWorkbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) createDataStyle(wb);
        style.setFillForegroundColor(new XSSFColor(COLOR_ZEBRA, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createNumberStyle(XSSFWorkbook wb) {
        CellStyle style = createDataStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createNumberStyleAlt(XSSFWorkbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) createNumberStyle(wb);
        style.setFillForegroundColor(new XSSFColor(COLOR_ZEBRA, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createPercentStyle(XSSFWorkbook wb) {
        CellStyle style = createNumberStyle(wb);
        style.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
        return style;
    }

    private CellStyle createPercentStyleAlt(XSSFWorkbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) createPercentStyle(wb);
        style.setFillForegroundColor(new XSSFColor(COLOR_ZEBRA, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createPositionStyle(XSSFWorkbook wb) {
        CellStyle style = createNumberStyle(wb);
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 13);
        font.setBold(true);
        font.setColor(new XSSFColor(COLOR_HEADER, null));
        style.setFont(font);
        return style;
    }

    private CellStyle createPositionStyleAlt(XSSFWorkbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) createPositionStyle(wb);
        style.setFillForegroundColor(new XSSFColor(COLOR_ZEBRA, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createSummaryLabelStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(COLOR_SUMMARY_LABEL, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(style, IndexedColors.WHITE.getIndex());
        return style;
    }

    private CellStyle createSummaryValueStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 18);
        font.setBold(true);
        font.setColor(new XSSFColor(COLOR_HEADER, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(COLOR_SUMMARY_BG, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
        applyThinBorders(style, IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle createSummaryPercentStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 18);
        font.setBold(true);
        font.setColor(new XSSFColor(COLOR_GREEN, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(COLOR_SUMMARY_BG, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(wb.createDataFormat().getFormat("0.0%"));
        applyThinBorders(style, IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle createMedalRowStyle(XSSFWorkbook wb, int position, boolean centered) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        if (centered) style.setAlignment(HorizontalAlignment.CENTER);

        byte[] color;
        switch (position) {
            case 1: color = new byte[]{(byte) 255, (byte) 249, (byte) 220}; break; // pale gold
            case 2: color = new byte[]{(byte) 245, (byte) 245, (byte) 245}; break; // pale silver
            case 3: color = new byte[]{(byte) 255, (byte) 237, (byte) 215}; break; // pale bronze
            default: color = new byte[]{(byte) 255, (byte) 255, (byte) 255}; break;
        }
        style.setFillForegroundColor(new XSSFColor(color, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorders(style, IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle createMedalPercentStyle(XSSFWorkbook wb, int position) {
        CellStyle style = createMedalRowStyle(wb, position, true);
        style.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
        return style;
    }

    private void applyThinBorders(CellStyle style, short colorIdx) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(colorIdx);
        style.setBottomBorderColor(colorIdx);
        style.setLeftBorderColor(colorIdx);
        style.setRightBorderColor(colorIdx);
    }

    // ==================== LEGACY / EMAIL SUPPORT ====================

    public String getResumoResultados() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RESULTADO DA ELEIÇÃO CIPA ===\n\n");

        List<Object[]> resultados = votacaoService.getResultados();
        long totalVotos = votacaoService.contarTotalVotos();
        long totalEleitores = usuarioService.contarTotalAtivos();

        sb.append("Total de votos: ").append(totalVotos).append("\n");
        sb.append("Total de eleitores: ").append(totalEleitores).append("\n");
        sb.append("Participação: ").append(String.format("%.1f%%", totalEleitores > 0 ? (totalVotos * 100.0 / totalEleitores) : 0)).append("\n\n");
        sb.append("=== RANKING ===\n\n");

        int posicao = 1;
        for (Object[] resultado : resultados) {
            Long candidatoId = (Long) resultado[0];
            Long votos = (Long) resultado[1];
            double percentual = totalVotos > 0 ? (votos * 100.0 / totalVotos) : 0;
            final int pos = posicao;

            candidatoService.buscarPorId(candidatoId).ifPresent(c -> {
                sb.append(pos).append("º - ").append(c.getNumero()).append(" - ").append(c.getNome())
                        .append(": ").append(votos).append(" votos (").append(String.format("%.1f%%", percentual)).append(")\n");
            });
            posicao++;
        }

        return sb.toString();
    }

    public void gerarEEnviarRelatorio() {
        log.info("Gerando relatório da eleição...");
        byte[] excel = gerarRelatorioExcel();
        emailService.enviarRelatorio(excel, null);
    }
}