package com.shiptrack.controller;

import com.shiptrack.model.Shipment;
import com.shiptrack.model.ShipmentStatus;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.service.ReportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    private Instant parse(String date, boolean endOfDay) {
        if (date == null || date.isBlank()) return null;
        LocalDate parsed = LocalDate.parse(date);
        return endOfDay
                ? parsed.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : parsed.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private ResponseEntity<Resource> file(byte[] bytes, String filename, MediaType type) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    @GetMapping("/shipments.pdf")
    public ResponseEntity<Resource> pdf(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestParam(required = false) ShipmentStatus status,
                                        @RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to,
                                        @RequestParam(defaultValue = "Shipment report") String title) {
        List<Shipment> shipments = reportService.scope(principal, status, parse(from, false), parse(to, true));
        return file(reportService.pdf(shipments, title), "shipment-report.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/shipments.xlsx")
    public ResponseEntity<Resource> excel(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam(required = false) ShipmentStatus status,
                                          @RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to,
                                          @RequestParam(defaultValue = "Shipment report") String title) {
        List<Shipment> shipments = reportService.scope(principal, status, parse(from, false), parse(to, true));
        return file(reportService.excel(shipments, title), "shipment-report.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/shipments.csv")
    public ResponseEntity<Resource> csv(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestParam(required = false) ShipmentStatus status,
                                        @RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to) {
        List<Shipment> shipments = reportService.scope(principal, status, parse(from, false), parse(to, true));
        return file(reportService.csv(shipments), "shipment-report.csv", MediaType.parseMediaType("text/csv"));
    }
}
