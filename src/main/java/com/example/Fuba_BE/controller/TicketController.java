package com.example.Fuba_BE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Ticket.TicketChangeRequestDTO;
import com.example.Fuba_BE.dto.Ticket.TicketChangeResponseDTO;
import com.example.Fuba_BE.dto.Ticket.TicketExportDTO;
import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.PdfService;
import com.example.Fuba_BE.service.Ticket.ITicketService;
import com.example.Fuba_BE.utils.QRCodeGenerator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

        private final ITicketService ticketService;

        @Autowired
        private PdfService pdfService;

        @GetMapping("/{ticketCode}")
        public ResponseEntity<ApiResponse<TicketScanResponseDTO>> getTicketByCode(@PathVariable String ticketCode) {
                TicketScanResponseDTO response = ticketService.getTicketDetailsByCode(ticketCode);
                return ResponseEntity.ok(ApiResponse.success("Ticket retrieved successfully", response));
        }

        /**
         * ========================================================================================
         * DEPRECATED: Check-in endpoints are no longer used in the simplified flow.
         *
         * NEW FLOW (Simplified):
         * 1. Staff scans QR code → GET /tickets/{ticketCode} (get ticket details)
         * 2. Staff confirms ticket → POST /tickets/{ticketCode}/confirm (set status to
         * "Used")
         *
         * Old flow had separate check-in step (Confirmed → CheckedIn → Used)
         * New flow is simpler: Confirmed → Used (one step)
         * ========================================================================================
         */

        // Check-in endpoints removed (kept in service as deprecated/legacy methods)

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

        /**
         * Staff confirms ticket usage (MAIN FLOW)
         *
         * Flow: Staff scans QR → sees ticket details → clicks confirm button
         * Status transition: Confirmed → Used
         *
         * This marks the ticket as used and passenger can board the bus.
         */
        @PostMapping("/{ticketCode}/confirm")
        public ResponseEntity<ApiResponse<Boolean>> confirmTicket(@PathVariable String ticketCode) {
                boolean isSuccess = ticketService.confirmTicket(ticketCode);
                return ResponseEntity.ok(ApiResponse.success("Soát vé thành công!", isSuccess));
        }

        @GetMapping("/{ticketId}/pdf")
        public ResponseEntity<byte[]> exportTicketPdf(@PathVariable Integer ticketId) {
                try {
                        TicketExportDTO realData = ticketService.getTicketExportData(ticketId);
                        byte[] pdfBytes = pdfService.generateTicketPdf(realData);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_PDF);
                        headers.setContentDispositionFormData("filename",
                                        "ticket_" + realData.getTicketCode() + ".pdf");

                        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
                } catch (Exception e) {
                        e.printStackTrace();
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
        }

        /**
         * Change ticket to a different trip on the same route
         * Only allows changing to trips on the same route
         * Admin and Staff can change tickets
         */
        @PutMapping("/{ticketId}/change")
        @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
        public ResponseEntity<ApiResponse<TicketChangeResponseDTO>> changeTicket(
                        @PathVariable Integer ticketId,
                        @Valid @RequestBody TicketChangeRequestDTO request) {
                // Set ticketId from path variable
                request.setTicketId(ticketId);

                TicketChangeResponseDTO response = ticketService.changeTicket(request);
                return ResponseEntity.ok(ApiResponse.success("Ticket changed successfully", response));
        }
}