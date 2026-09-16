package com.restaurante.web.cash;

import com.restaurante.application.payment.InvoicePdfService;
import com.restaurante.application.payment.InvoiceService;
import com.restaurante.web.dto.invoice.InvoiceResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/caja/facturas")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    public InvoiceController(
            InvoiceService invoiceService,
            InvoicePdfService invoicePdfService) {

        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping("/{facturaId}")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable Long facturaId) {

        return ResponseEntity.ok(
                invoiceService.getInvoice(facturaId)
        );
    }

    @GetMapping(
            value = "/{facturaId}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> getInvoicePdf(
            @PathVariable Long facturaId) {

        InvoiceResponse invoice =
                invoiceService.getInvoice(facturaId);

        byte[] pdf =
                invoicePdfService.generate(invoice);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + invoice.numeroDocumento()
                                + ".pdf\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}