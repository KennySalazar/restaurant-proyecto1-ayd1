package com.restaurante.application.report.export;

import com.restaurante.exception.ApiException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class ReportExcelExportService {

    public byte[] generate(ReportExportData data) {

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {
            Sheet sheet =
                    workbook.createSheet("Reporte");

            int rowIndex = 0;

            Row titleRow =
                    sheet.createRow(rowIndex++);

            titleRow.createCell(0)
                    .setCellValue(data.titulo());

            for (String criterio : data.criterios()) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0)
                        .setCellValue(criterio);
            }

            rowIndex++;

            CellStyle headerStyle =
                    workbook.createCellStyle();

            Font headerFont =
                    workbook.createFont();

            headerFont.setBold(true);

            headerStyle.setFont(headerFont);

            Row headerRow =
                    sheet.createRow(rowIndex++);

            for (int i = 0;
                 i < data.encabezados().size();
                 i++) {

                Cell cell =
                        headerRow.createCell(i);

                cell.setCellValue(
                        data.encabezados().get(i)
                );

                cell.setCellStyle(headerStyle);
            }

            if (data.filas().isEmpty()) {

                Row emptyRow =
                        sheet.createRow(rowIndex);

                emptyRow.createCell(0)
                        .setCellValue(
                                "No existen datos para los criterios seleccionados."
                        );

            } else {

                for (var values : data.filas()) {

                    Row row =
                            sheet.createRow(rowIndex++);

                    for (int i = 0;
                         i < values.size();
                         i++) {

                        row.createCell(i)
                                .setCellValue(
                                        values.get(i) == null
                                                ? ""
                                                : values.get(i)
                                );
                    }
                }
            }

            for (int i = 0;
                 i < data.encabezados().size();
                 i++) {

                sheet.autoSizeColumn(i);
            }

            workbook.write(output);

            return output.toByteArray();

        } catch (Exception exception) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "report_export_failed",
                    "Error de exportacion",
                    "No fue posible generar el archivo Excel"
            );
        }
    }
}