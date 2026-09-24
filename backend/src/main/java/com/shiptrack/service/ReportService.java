package com.shiptrack.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shiptrack.model.Shipment;
import com.shiptrack.model.ShipmentStatus;
import com.shiptrack.repository.ShipmentRepository;
import com.shiptrack.security.UserPrincipal;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class ReportService {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());

    private static final String[] HEADERS = {
            "Tracking number", "Status", "Service", "Origin", "Destination",
            "Driver", "Distance (km)", "Created", "Promised", "Delivered", "Delay (min)"
    };

    private final ShipmentRepository shipmentRepository;
    private final AnalyticsService analyticsService;

    public ReportService(ShipmentRepository shipmentRepository, AnalyticsService analyticsService) {
        this.shipmentRepository = shipmentRepository;
        this.analyticsService = analyticsService;
    }

    public List<Shipment> scope(UserPrincipal principal, ShipmentStatus status, Instant from, Instant to) {
        List<Shipment> shipments = principal.isStaff()
                ? shipmentRepository.findAll()
                : shipmentRepository.findByOwnerId(principal.getId());

        return shipments.stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> from == null || (s.getCreatedAt() != null && !s.getCreatedAt().isBefore(from)))
                .filter(s -> to == null || (s.getCreatedAt() != null && s.getCreatedAt().isBefore(to)))
                .sorted(Comparator.comparing(Shipment::getCreatedAt).reversed())
                .toList();
    }

    private String[] row(Shipment s) {
        return new String[]{
                s.getTrackingNumber(),
                s.getStatus().name(),
                s.getServiceType(),
                s.getSender() == null ? "" : s.getSender().shortText(),
                s.getReceiver() == null ? "" : s.getReceiver().shortText(),
                s.getAssignedDriverName() == null ? "Unassigned" : s.getAssignedDriverName(),
                s.getDistanceKm() == null ? "0" : String.valueOf(s.getDistanceKm()),
                s.getCreatedAt() == null ? "" : STAMP.format(s.getCreatedAt()),
                s.getOriginalEstimatedDeliveryAt() == null ? "" : STAMP.format(s.getOriginalEstimatedDeliveryAt()),
                s.getActualDeliveryAt() == null ? "" : STAMP.format(s.getActualDeliveryAt()),
                String.valueOf(s.getDelayMinutes())
        };
    }

    // -------------------------------------------------------------------- CSV

    public byte[] csv(List<Shipment> shipments) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append('\n');
        for (Shipment shipment : shipments) {
            String[] values = row(shipment);
            for (int i = 0; i < values.length; i++) {
                sb.append('"').append(values[i] == null ? "" : values[i].replace("\"", "\"\"")).append('"');
                if (i < values.length - 1) sb.append(',');
            }
            sb.append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ Excel

    public byte[] excel(List<Shipment> shipments, String title) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Shipments");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(title + " - generated " + STAMP.format(Instant.now()));

            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 3;
            for (Shipment shipment : shipments) {
                Row row = sheet.createRow(rowIndex++);
                String[] values = row(shipment);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }

            Row summary = sheet.createRow(rowIndex + 1);
            summary.createCell(0).setCellValue("Total shipments");
            summary.createCell(1).setCellValue(shipments.size());

            Row onTime = sheet.createRow(rowIndex + 2);
            onTime.createCell(0).setCellValue("On-time rate (%)");
            onTime.createCell(1).setCellValue(analyticsService.onTimeRate(shipments));

            Row avg = sheet.createRow(rowIndex + 3);
            avg.createCell(0).setCellValue("Average delivery time (hours)");
            avg.createCell(1).setCellValue(analyticsService.averageDeliveryHours(shipments));

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("The spreadsheet could not be generated", ex);
        }
    }

    // -------------------------------------------------------------------- PDF

    public byte[] pdf(List<Shipment> shipments, String title) {
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 32, 32);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(17, 39, 51));
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(90, 100, 110));
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

        Paragraph heading = new Paragraph(title, titleFont);
        heading.setSpacingAfter(4);
        document.add(heading);

        document.add(new Paragraph("ShipTrack Pro - generated " + STAMP.format(Instant.now()), metaFont));
        document.add(new Paragraph(" ", metaFont));

        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, headFont));
            cell.setBackgroundColor(new Color(17, 39, 51));
            cell.setPadding(5);
            cell.setBorderColor(new Color(17, 39, 51));
            table.addCell(cell);
        }

        boolean shaded = false;
        for (Shipment shipment : shipments) {
            for (String value : row(shipment)) {
                PdfPCell cell = new PdfPCell(new Paragraph(value == null ? "" : value, cellFont));
                cell.setPadding(4);
                cell.setBorderColor(new Color(220, 225, 230));
                if (shaded) cell.setBackgroundColor(new Color(246, 248, 250));
                table.addCell(cell);
            }
            shaded = !shaded;
        }
        document.add(table);

        Paragraph footer = new Paragraph(String.format(
                "%d shipments  |  on-time rate %.1f%%  |  average delivery %.1f hours",
                shipments.size(),
                analyticsService.onTimeRate(shipments),
                analyticsService.averageDeliveryHours(shipments)), metaFont);
        footer.setSpacingBefore(12);
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }
}
