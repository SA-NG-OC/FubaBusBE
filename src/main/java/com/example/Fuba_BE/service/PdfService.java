package com.example.Fuba_BE.service;

import com.example.Fuba_BE.dto.Ticket.TicketExportDTO;
import com.example.Fuba_BE.utils.QRCodeGenerator;
import com.lowagie.text.pdf.BaseFont;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource; // <--- Import cái này
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;

    public byte[] generateTicketPdf(TicketExportDTO ticketData) throws Exception {

        // 1. Tạo QR Code
        byte[] qrBytes = QRCodeGenerator.generateQRCodeImage(ticketData.getTicketCode(), 200, 200);
        String qrBase64 = Base64.getEncoder().encodeToString(qrBytes);
        ticketData.setQrCodeBase64(qrBase64);

        // 2. Render HTML
        Context context = new Context();
        context.setVariable("ticket", ticketData);
        String htmlContent = templateEngine.process("ticket-pdf", context);

        // 3. Tạo PDF Renderer
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

        // --- KHẮC PHỤC LỖI FONT Ở ĐÂY ---
        // Lấy đường dẫn tuyệt đối tới file font trong resources
        String fontPath = new ClassPathResource("fonts/arial.ttf").getURL().toExternalForm();

        renderer.getFontResolver().addFont(
                fontPath,
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED
        );
        // --------------------------------

        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }
}