package com.restaurante.application.report.export;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.restaurante.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class ReportPdfExportService {

    public byte[] generate(ReportExportData data) {

        try {
            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            Document document = new Document();

            PdfWriter.getInstance(
                    document,
                    output
            );

            document.open();

            document.add(
                    new Paragraph(data.titulo())
            );

            document.add(new Paragraph(" "));

            for (String criterio : data.criterios()) {
                document.add(
                        new Paragraph(criterio)
                );
            }

            document.add(new Paragraph(" "));

            if (data.filas().isEmpty()) {
                document.add(
                        new Paragraph(
                                "No existen datos para los criterios seleccionados."
                        )
                );
            } else {
                PdfPTable table =
                        new PdfPTable(
                                data.encabezados().size()
                        );

                table.setWidthPercentage(100);

                for (String header : data.encabezados()) {
                    table.addCell(header);
                }

                for (var row : data.filas()) {
                    for (String value : row) {
                        table.addCell(
                                value == null ? "" : value
                        );
                    }
                }

                document.add(table);
            }

            document.close();

            return output.toByteArray();

        } catch (Exception exception) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "report_export_failed",
                    "Error de exportacion",
                    "No fue posible generar el archivo PDF"
            );
        }
    }
}