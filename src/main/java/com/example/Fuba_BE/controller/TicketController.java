package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Ticket.ITicketService;
import com.example.Fuba_BE.utils.QRCodeGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket Management", description = "APIs for ticket scanning and details")
public class TicketController {

    private final ITicketService ticketService;

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

    @Operation(summary = "Generate QR Code", description = "Generates a QR code image linking to the ticket details")
    @GetMapping(value = "/{ticketCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateTicketQR(@PathVariable String ticketCode) {
        try {
            String infoToEmbed = "http://127.0.0.1:5500/ticket.html?code=" + ticketCode;

            byte[] qrImage = QRCodeGenerator.generateQRCodeImage(infoToEmbed, 300, 300);

            return ResponseEntity.ok().body(qrImage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}