package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.domain.entity.Ticket;
import com.example.Fuba_BE.dto.Ticket.TicketCheckInRequestDTO;
import com.example.Fuba_BE.dto.Ticket.TicketCheckInResponseDTO;
import com.example.Fuba_BE.dto.Ticket.TicketExportDTO;
import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.PdfService;
import com.example.Fuba_BE.service.Ticket.ITicketService;
import com.example.Fuba_BE.utils.QRCodeGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket Management", description = "APIs for ticket scanning, check-in, and details")
public class TicketController {

    private final ITicketService ticketService;
    @Autowired
    private PdfService pdfService;

    @Operation(summary = "Get ticket details by code", description = "Returns detailed ticket information for scanning (QR Code)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Ticket retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TicketScanResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Ticket code not found"
            )
    })
    @GetMapping("/{ticketCode}")
    public ResponseEntity<ApiResponse<TicketScanResponseDTO>> getTicketByCode(@PathVariable String ticketCode) {
        TicketScanResponseDTO response = ticketService.getTicketDetailsByCode(ticketCode);

        return ResponseEntity.ok(
                ApiResponse.success("Ticket retrieved successfully", response)
        );
    }

    @Operation(summary = "Check-in ticket via QR code",
            description = "Validates and marks a ticket as checked-in (Used) after QR code scan")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Check-in successful",
                    content = @Content(schema = @Schema(implementation = TicketCheckInResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid ticket status or check-in time"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found"
            )
    })
    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<TicketCheckInResponseDTO>> checkInTicket(
            @Valid @RequestBody TicketCheckInRequestDTO request) {
        TicketCheckInResponseDTO response = ticketService.checkInTicket(request);

        return ResponseEntity.ok(
                ApiResponse.success("Check-in successful", response)
        );
    }

    @Operation(summary = "Quick check-in by ticket code",
            description = "Quick check-in using ticket code with optional trip validation. " +
                    "If tripId is provided, validates that ticket belongs to the correct trip.")
    @PostMapping("/check-in/{ticketCode}")
    public ResponseEntity<ApiResponse<TicketCheckInResponseDTO>> quickCheckIn(
            @PathVariable String ticketCode,
            @RequestParam(required = false) Integer tripId,
            @RequestParam(required = false) Integer vehicleId) {
        TicketCheckInRequestDTO request = TicketCheckInRequestDTO.builder()
                .ticketCode(ticketCode)
                .tripId(tripId)
                .vehicleId(vehicleId)
                .checkInMethod("QR")
                .build();

        TicketCheckInResponseDTO response = ticketService.checkInTicket(request);

        return ResponseEntity.ok(
                ApiResponse.success("Check-in successful", response)
        );
    }

    @Operation(summary = "Generate QR Code")
    @GetMapping(value = "/{ticketCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateTicketQR(@PathVariable String ticketCode) {
        try {
            String publicUrl = "http://127.0.0.1:5500/ticket.html";
            String qrContent = publicUrl + "?ticketCode=" + ticketCode;

            byte[] qrImage = QRCodeGenerator.generateQRCodeImage(qrContent, 300, 300);

            return ResponseEntity.ok().body(qrImage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Staff confirm ticket")
    @PostMapping("/{ticketCode}/confirm")
    public ResponseEntity<ApiResponse<Boolean>> confirmTicket(@PathVariable String ticketCode) {
        boolean isSuccess = ticketService.confirmTicket(ticketCode);
        return ResponseEntity.ok(ApiResponse.success("Soát vé thành công!", isSuccess));
    }

    @Operation(summary = "Export Ticket PDF", description = "Generate and download a PDF ticket with real data from DB")
    @GetMapping("/{ticketId}/pdf")
    public ResponseEntity<byte[]> exportTicketPdf(@PathVariable Integer ticketId) {
        try {
            // 1. Gọi Service lấy data thật từ DB
            TicketExportDTO realData = ticketService.getTicketExportData(ticketId);

            // 2. Tạo PDF từ data thật
            byte[] pdfBytes = pdfService.generateTicketPdf(realData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // "inline" để xem trên trình duyệt, "attachment" để tải về
            headers.setContentDispositionFormData("filename", "ticket_" + realData.getTicketCode() + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}