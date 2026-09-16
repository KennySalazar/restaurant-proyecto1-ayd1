package com.restaurante.application.payment;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.restaurante.web.dto.invoice.InvoiceDetailResponse;
import com.restaurante.web.dto.invoice.InvoicePaymentResponse;
import com.restaurante.web.dto.invoice.InvoiceResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Service
public class InvoicePdfService {

    public byte[] generate(InvoiceResponse invoice) {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        Document document = new Document();

        PdfWriter.getInstance(document, output);

        document.open();

        document.add(new Paragraph(
                invoice.restauranteNombreComercial()
        ));

        document.add(new Paragraph(
                "Comprobante: " + invoice.numeroDocumento()
        ));

        document.add(new Paragraph(
                "Cuenta: " + invoice.numeroCuenta()
        ));

        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);

        table.addCell("Producto");
        table.addCell("Cantidad");
        table.addCell("Precio");
        table.addCell("Subtotal");

        for (InvoiceDetailResponse detail : invoice.detalles()) {
            table.addCell(detail.nombre());
            table.addCell(detail.cantidad().toPlainString());
            table.addCell(money(detail.precioUnitario()));
            table.addCell(money(detail.subtotalLinea()));
        }

        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(
                "Subtotal: " + money(invoice.subtotal())
        ));
        document.add(new Paragraph(
                "Descuentos: " + money(invoice.descuentoTotal())
        ));
        document.add(new Paragraph(
                "Impuesto: " + money(invoice.montoImpuesto())
        ));
        document.add(new Paragraph(
                "Propina: " + money(invoice.montoPropina())
        ));
        document.add(new Paragraph(
                "TOTAL: " + money(invoice.total())
        ));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Metodos de pago:"));

        for (InvoicePaymentResponse payment : invoice.pagos()) {
            document.add(new Paragraph(
                    payment.metodoNombre()
                            + ": "
                            + money(payment.monto())
            ));
        }

        if (invoice.puntosOtorgados() > 0) {
            document.add(new Paragraph(
                    "Puntos obtenidos: "
                            + invoice.puntosOtorgados()
            ));
        }

        if (invoice.puntosRedimidos() > 0) {
            document.add(new Paragraph(
                    "Puntos redimidos: "
                            + invoice.puntosRedimidos()
            ));
        }

        document.close();

        return output.toByteArray();
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }

        return value.setScale(2).toPlainString();
    }
}