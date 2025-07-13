package com.dazzling.erp.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import com.dazzling.erp.models.Lot;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

/**
 * Utility class for generating professional PDF reports for lot details
 */
public class PdfGenerator {
    
    private static final String TAG = "PdfGenerator";
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;
    private static final int LINE_HEIGHT = 16;
    private static final int SECTION_SPACING = 25;
    private static final int SUBSECTION_SPACING = 15;
    private static final int FOOTER_HEIGHT = 35; // Space reserved for footer elements
    
    private final Context context;
    private PDDocument document;
    private PDPageContentStream contentStream;
    private float currentY;
    
    public PdfGenerator(Context context) {
        this.context = context;
        this.document = new PDDocument();
        // Initialize PDFBox
        PDFBoxResourceLoader.init(context);
    }
    
    /**
     * Generate PDF for a single lot
     */
    public void generatePdf(Lot lot, OutputStream outputStream) throws IOException {
        document = new PDDocument();
        try {
            createDynamicLotReport(lot);
            document.save(outputStream);
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }
    
    /**
     * Generate unique filename for the PDF
     */
    private String generateFileName(Lot lot) {
        String lotName = lot.getLotNumber();
        if (lotName == null || lotName.trim().isEmpty()) {
            lotName = "Unknown";
        }
        
        // Clean lot name for filename (remove special characters)
        lotName = lotName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        
        return "Lot_" + lotName + "_" + timestamp + ".pdf";
    }
    
    /**
     * Generate a professional PDF report for a lot and save to Downloads
     */
    public String generateLotReportToDownloads(Lot lot) throws IOException {
        try {
            // Generate PDF to ByteArrayOutputStream first
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            generatePdf(lot, baos);
            
            // Save to Downloads using MediaStore
            String fileName = generateFileName(lot);
            Uri fileUri = savePdfToDownloads(fileName, baos.toByteArray());
            
            Log.d(TAG, "PDF generated and saved successfully: " + fileUri);
            return fileUri.toString();
            
        } catch (IOException e) {
            Log.e(TAG, "Error generating PDF", e);
            throw e;
        }
    }
    
    /**
     * Generate a professional PDF report for all lots (each lot per page) and save to Downloads
     */
    public String generateAllLotsReportToDownloads(List<Lot> lots) throws IOException {
        if (lots == null || lots.isEmpty()) {
            throw new IOException("No lots to generate PDF");
        }
        document = new PDDocument();
        try {
            for (Lot lot : lots) {
                createDynamicLotReport(lot); // Each call adds a new page for the lot
            }
            // Draw correct page numbers on all pages
            int totalPages = document.getNumberOfPages();
            for (int i = 0; i < totalPages; i++) {
                PDPage page = document.getPage(i);
                PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
                drawCustomFooter(cs, i + 1, totalPages);
                cs.close();
            }
            // Generate a filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "All_Lots_Report_" + timestamp + ".pdf";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            Uri fileUri = savePdfToDownloads(fileName, baos.toByteArray());
            Log.d(TAG, "All lots PDF generated and saved: " + fileUri);
            return fileUri.toString();
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }
    
    /**
     * Save PDF bytes to Downloads folder using MediaStore
     */
    private Uri savePdfToDownloads(String fileName, byte[] pdfBytes) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }

        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(pdfBytes);
                    outputStream.flush();
                }
            }
            
            // Clear the pending flag
            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
            
            return uri;
        }
        
        throw new IOException("Failed to create file in Downloads");
    }
    
    /**
     * Create the title page (compact, minimal blank space)
     */
    private void createTitlePage(Lot lot, int pageNum, int totalPages) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);

        // Start with top margin, reserve space for footer
        currentY = PAGE_HEIGHT - MARGIN;

        // Company Header
        addCenteredText("DAZZLING FASHION BD", 18, true);
        currentY -= 10;
        addCenteredText("Enterprise Resource Planning System", 12, false);
        currentY -= 12;

        // Report Title
        addCenteredText("LOT DETAIL REPORT", 20, true);
        currentY -= 16;

        // Lot Information
        addText("Lot Number:", MARGIN, currentY, 11, true);
        addText(lot.getLotNumber(), MARGIN + 90, currentY, 11, false);
        currentY -= LINE_HEIGHT;

        addText("Report Date:", MARGIN, currentY, 11, true);
        addText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()),
                MARGIN + 90, currentY, 11, false);
        currentY -= LINE_HEIGHT;

        addText("Generated By:", MARGIN, currentY, 11, true);
        addText("ERP System", MARGIN + 90, currentY, 11, false);
        currentY -= 12;

        // Footer: page number
        drawCustomFooter(contentStream, pageNum, totalPages);
        contentStream.close();
    }
    
    /**
     * Create the summary page
     */
    private void createSummaryPage(Lot lot, int pageNum, int totalPages) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        currentY = PAGE_HEIGHT - MARGIN;
        
        // Page Title
        addCenteredText("EXECUTIVE SUMMARY", 18, true);
        currentY -= 10;
        
        // Summary Box
        addText("Lot Overview:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("• Lot Number: " + lot.getLotNumber(), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Order Date: " + formatDate(lot.getOrderDate()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Delivery Date: " + formatDate(lot.getDeliveryDate()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Status: " + (lot.getStatus() != null ? lot.getStatus().toUpperCase() : "ACTIVE"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Priority: " + (lot.getPriority() != null ? lot.getPriority().toUpperCase() : "MEDIUM"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Quality Grade: " + (lot.getQuality() != null ? lot.getQuality().toUpperCase() : "A"), MARGIN + 20, currentY, 11, false);
        currentY -= SECTION_SPACING;
        
        // Fabric Information
        addText("Fabric Details:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("• Fabric Type: " + (lot.getFabricType() != null ? lot.getFabricType() : "Not specified"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Color: " + (lot.getColor() != null ? lot.getColor() : "Not specified"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Supplier: " + (lot.getSupplier() != null ? lot.getSupplier() : "Not specified"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Customer: " + (lot.getCustomer() != null ? lot.getCustomer() : "Not specified"), MARGIN + 20, currentY, 11, false);
        currentY -= SECTION_SPACING;
        
        // Quantities Summary
        addText("Quantity Summary:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("• Total Fabric: " + formatQuantityKg(lot.getTotalFabricKg()) + " KG", MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Cutting: " + formatQuantityWithBoth(lot.getCuttingKg(), lot.getCuttingPcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Embroidery Receive: " + formatQuantityWithBoth(lot.getEmbroideryReceiveKg(), lot.getEmbroideryReceivePcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Embroidery Reject: " + formatQuantityWithBoth(lot.getEmbroideryRejectKg(), lot.getEmbroideryRejectPcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("• Office Shipment: " + formatQuantityWithBoth(lot.getOfficeShipmentKg(), lot.getOfficeShipmentPcs()), MARGIN + 20, currentY, 11, false);
        
        // Footer: page number
        drawCustomFooter(contentStream, pageNum, totalPages);
        contentStream.close();
    }
    
    /**
     * Create the detailed information page
     */
    private void createDetailedInfoPage(Lot lot, int pageNum, int totalPages) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        currentY = PAGE_HEIGHT - MARGIN;
        
        // Page Title
        addCenteredText("DETAILED INFORMATION", 18, true);
        currentY -= 10;
        
        // Basic Information
        addText("Basic Information:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("Lot Number: " + lot.getLotNumber(), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Order Date: " + formatDate(lot.getOrderDate()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Delivery Date: " + formatDate(lot.getDeliveryDate()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Status: " + (lot.getStatus() != null ? lot.getStatus().toUpperCase() : "ACTIVE"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Priority: " + (lot.getPriority() != null ? lot.getPriority().toUpperCase() : "MEDIUM"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Quality Grade: " + (lot.getQuality() != null ? lot.getQuality().toUpperCase() : "A"), MARGIN + 20, currentY, 11, false);
        currentY -= SECTION_SPACING;
        
        // Fabric Information
        addText("Fabric Information:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("Fabric Type: " + (lot.getFabricType() != null ? lot.getFabricType() : "Not specified"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Color: " + (lot.getColor() != null ? lot.getColor() : "Not specified"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Supplier: " + (lot.getSupplier() != null ? lot.getSupplier() : "Not specified"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Customer: " + (lot.getCustomer() != null ? lot.getCustomer() : "Not specified"), MARGIN + 20, currentY, 11, false);
        currentY -= SECTION_SPACING;
        
        // Quantity Details
        addText("Quantity Details:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("Total Fabric: " + formatQuantityKg(lot.getTotalFabricKg()) + " KG", MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Cutting (KG): " + formatQuantityKg(lot.getCuttingKg()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Cutting (PCS): " + formatQuantityPcs(lot.getCuttingPcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Embroidery Receive (KG): " + formatQuantityKg(lot.getEmbroideryReceiveKg()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Embroidery Receive (PCS): " + formatQuantityPcs(lot.getEmbroideryReceivePcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Embroidery Reject (KG): " + formatQuantityKg(lot.getEmbroideryRejectKg()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Embroidery Reject (PCS): " + formatQuantityPcs(lot.getEmbroideryRejectPcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Office Shipment (KG): " + formatQuantityKg(lot.getOfficeShipmentKg()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Office Shipment (PCS): " + formatQuantityPcs(lot.getOfficeShipmentPcs()), MARGIN + 20, currentY, 11, false);
        currentY -= SECTION_SPACING;
        
        // Additional Information
        addText("Additional Information:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("Notes: " + (lot.getNotes() != null ? lot.getNotes() : "No notes available"), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Created Date: " + formatDate(lot.getCreatedAt()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Last Updated: " + formatDate(lot.getUpdatedAt()), MARGIN + 20, currentY, 11, false);
        
        // Footer: page number
        drawCustomFooter(contentStream, pageNum, totalPages);
        contentStream.close();
    }
    
    /**
     * Create cutting details page
     */
    private void createCuttingDetailsPage(Lot lot, int pageNum, int totalPages, boolean isLastPage) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        currentY = PAGE_HEIGHT - MARGIN;
        
        // Page Title
        addCenteredText("CUTTING OPERATIONS", 18, true);
        currentY -= 10;
        
        // Cutting Information
        addText("Cutting Details:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("Cutting Quantity: " + formatQuantityWithBoth(lot.getCuttingKg(), lot.getCuttingPcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Cutting Start Date: " + formatDate(lot.getCuttingStartDate()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Cutting End Date: " + formatDate(lot.getCuttingEndDate()), MARGIN + 20, currentY, 11, false);
        currentY -= SECTION_SPACING;
        
        // Embroidery Information
        addText("Embroidery Details:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("Embroidery Receive: " + formatQuantityWithBoth(lot.getEmbroideryReceiveKg(), lot.getEmbroideryReceivePcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Embroidery Reject: " + formatQuantityWithBoth(lot.getEmbroideryRejectKg(), lot.getEmbroideryRejectPcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Embroidery Start Date: " + formatDate(lot.getEmbroideryStartDate()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Embroidery End Date: " + formatDate(lot.getEmbroideryEndDate()), MARGIN + 20, currentY, 11, false);
        currentY -= SECTION_SPACING;
        
        // Shipment Information
        addText("Shipment Details:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("Office Shipment: " + formatQuantityWithBoth(lot.getOfficeShipmentKg(), lot.getOfficeShipmentPcs()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Shipment Date: " + formatDate(lot.getShipmentDate()), MARGIN + 20, currentY, 11, false);
        currentY -= LINE_HEIGHT;
        
        addText("Delivery Date: " + formatDate(lot.getDeliveryDate()), MARGIN + 20, currentY, 11, false);
        currentY -= SECTION_SPACING;
        
        // Timeline
        addText("Production Timeline:", MARGIN, currentY, 14, true);
        currentY -= LINE_HEIGHT;
        
        addText("1. Order Received: " + formatDate(lot.getOrderDate()), MARGIN + 20, currentY, 12, false);
        currentY -= LINE_HEIGHT;
        
        addText("2. Cutting Started: " + formatDate(lot.getCuttingStartDate()), MARGIN + 20, currentY, 12, false);
        currentY -= LINE_HEIGHT;
        
        addText("3. Cutting Completed: " + formatDate(lot.getCuttingEndDate()), MARGIN + 20, currentY, 12, false);
        currentY -= LINE_HEIGHT;
        
        addText("4. Embroidery Started: " + formatDate(lot.getEmbroideryStartDate()), MARGIN + 20, currentY, 12, false);
        currentY -= LINE_HEIGHT;
        
        addText("5. Embroidery Completed: " + formatDate(lot.getEmbroideryEndDate()), MARGIN + 20, currentY, 12, false);
        currentY -= LINE_HEIGHT;
        
        addText("6. Shipment Date: " + formatDate(lot.getShipmentDate()), MARGIN + 20, currentY, 12, false);
        currentY -= LINE_HEIGHT;
        
        addText("7. Delivery Date: " + formatDate(lot.getDeliveryDate()), MARGIN + 20, currentY, 12, false);
        currentY -= 40;
        
        // Notes Section
        addText("Additional Notes:", MARGIN, currentY, 16, true);
        currentY -= LINE_HEIGHT;
        
        String notes = lot.getNotes() != null && !lot.getNotes().isEmpty() ? lot.getNotes() : "No additional notes available.";
        addWrappedText(notes, MARGIN + 20, currentY, 12, false, PAGE_WIDTH - 2 * MARGIN - 40);
        
        // Footer: page number
        drawCustomFooter(contentStream, pageNum, totalPages);
        contentStream.close();
    }
    
    /**
     * Add centered text
     */
    private void addCenteredText(String text, int fontSize, boolean bold) throws IOException {
        PDType1Font font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(0, 0, 0); // Black color
        
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(x, currentY);
        contentStream.showText(text);
        contentStream.endText();
        
        currentY -= fontSize + 5;
    }
    
    /**
     * Add left-aligned text
     */
    private void addText(String text, float x, float y, int fontSize, boolean bold) throws IOException {
        PDType1Font font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(0, 0, 0); // Black color
        
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }
    
    /**
     * Add wrapped text for long content
     */
    private void addWrappedText(String text, float x, float y, int fontSize, boolean bold, float maxWidth) throws IOException {
        PDType1Font font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(0, 0, 0); // Black color
        
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float currentY = y;
        
        for (String word : words) {
            String testLine = line.toString() + " " + word;
            float lineWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            
            if (lineWidth > maxWidth && line.length() > 0) {
                // Draw the current line
                contentStream.beginText();
                contentStream.newLineAtOffset(x, currentY);
                contentStream.showText(line.toString().trim());
                contentStream.endText();
                
                // Start new line
                line = new StringBuilder(word);
                currentY -= fontSize + 2;
            } else {
                line.append(" ").append(word);
            }
        }
        
        // Draw the last line
        if (line.length() > 0) {
            contentStream.beginText();
            contentStream.newLineAtOffset(x, currentY);
            contentStream.showText(line.toString().trim());
            contentStream.endText();
        }
    }
    
    /**
     * Format date safely
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "Not specified";
        }
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
    }
    
    /**
     * Format quantity in KG with proper validation
     */
    private String formatQuantityKg(double quantity) {
        if (quantity <= 0) {
            return "N/A";
        }
        return String.format(Locale.getDefault(), "%.2f", quantity);
    }
    
    /**
     * Format quantity in Pcs with proper validation
     */
    private String formatQuantityPcs(int quantity) {
        if (quantity <= 0) {
            return "N/A";
        }
        return String.valueOf(quantity);
    }
    
    /**
     * Format quantity with both KG and Pcs
     */
    private String formatQuantityWithBoth(double kgQuantity, int pcsQuantity) {
        String kgStr = formatQuantityKg(kgQuantity);
        String pcsStr = formatQuantityPcs(pcsQuantity);
        
        if (kgStr.equals("N/A") && pcsStr.equals("N/A")) {
            return "N/A";
        } else if (kgStr.equals("N/A")) {
            return pcsStr + " Pcs";
        } else if (pcsStr.equals("N/A")) {
            return kgStr + " KG";
        } else {
            return kgStr + " KG (" + pcsStr + " Pcs)";
        }
    }

    /**
     * Draw page number footer at bottom center
     */
    private void drawPageNumberFooter(PDPageContentStream cs, int pageNum, int totalPages) throws IOException {
        String text = "Page " + pageNum + " of " + totalPages;
        PDType1Font font = PDType1Font.HELVETICA;
        int fontSize = 9;
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2;
        float y = 25; // 25pt from bottom
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(0.3f, 0.3f, 0.3f); // Dark gray
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    /**
     * Draw signature at bottom left
     */
    private void drawSignature(PDPageContentStream cs) throws IOException {
        String signature = "Liyon";
        PDType1Font font = PDType1Font.HELVETICA_OBLIQUE;
        int fontSize = 10;
        float x = MARGIN;
        float y = 15; // 15pt from bottom
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(0.2f, 0.2f, 0.2f); // Dark gray
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(signature);
        cs.endText();
    }

    /**
     * Draw developer credit at bottom center
     */
    private void drawDeveloperCredit(PDPageContentStream cs) throws IOException {
        String credit = "Developed by Mushfiquzzaman Liyon";
        PDType1Font font = PDType1Font.HELVETICA;
        int fontSize = 8;
        float textWidth = font.getStringWidth(credit) / 1000 * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2;
        float y = 8; // 8pt from bottom
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(0.5f, 0.5f, 0.5f); // Medium gray
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(credit);
        cs.endText();
    }

    // --- NEW: Dynamic attractive layout with 1 or 2 pages ---
    private void createDynamicLotReport(Lot lot) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        // Add extra top padding (80pt) for a larger top margin
        currentY = PAGE_HEIGHT - MARGIN - 80;
        contentStream = new PDPageContentStream(document, page);

        // --- Custom header with logo and title ---
        drawLogoAndTitleHeader("Lot Details Report");
        currentY -= 60; // Increase space after header title for more bottom margin

        // Lot Info (header row with actual lot number)
        float infoLabelFontSize = 12f;
        float infoValueFontSize = 12f;
        float infoY = currentY;
        // Lot number (left, with value)
        String lotNumberLabel = "Lot number: ";
        String lotNumberValue = lot.getLotNumber() != null ? lot.getLotNumber() : "";
        String lotNumberText = lotNumberLabel + lotNumberValue;
        contentStream.setFont(PDType1Font.HELVETICA, infoLabelFontSize);
        contentStream.setNonStrokingColor(38f/255, 50f/255, 56f/255); // #263238
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, infoY);
        contentStream.showText(lotNumberText);
        contentStream.endText();
        // Report Date (right)
        String reportDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        float reportDateWidth = PDType1Font.HELVETICA.getStringWidth("Report Date: " + reportDate) / 1000 * infoValueFontSize;
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_WIDTH - MARGIN - reportDateWidth, infoY);
        contentStream.showText("Report Date: " + reportDate);
        contentStream.endText();
        currentY -= 22;
        drawCustomSeparator(currentY);
        currentY -= 18;

        // Quantity Summary
        addSectionTitleLeft("Quantity Summary");
        addFieldLabelValueRow("Cutting Quantity (PCS)", String.valueOf(lot.getCuttingPcs()));
        currentY -= 18;
        drawCustomSeparator(currentY);
        currentY -= 18;

        // Quantity Details (Embroidery)
        addSectionTitleLeft("Quantity Details");
        addFieldLabelValueRow("Embroidery  Revice (PCS)", String.valueOf(lot.getEmbroideryReceivePcs()));
        addFieldLabelValueRow("Embroidery  Reject (PCS)", String.valueOf(lot.getEmbroideryRejectPcs()));
        currentY -= 18;
        drawCustomSeparator(currentY);
        currentY -= 18;

        // Office Shipment
        addSectionTitleLeft("Office Shipment");
        addFieldLabelValueRow("Office Quantity (PCS)", String.valueOf(lot.getOfficeShipmentPcs()));
        currentY -= 18;
        drawCustomSeparator(currentY);
        currentY -= 18;

        // Factory Details
        addSectionTitleLeft("Factory Details");
        addFieldLabelValueRow("Total Balance (PCS)", String.valueOf(lot.getTotalFactoryBalancePcs()));
        addFieldLabelValueRow("A Grade Quantity (PCS)", String.valueOf(lot.getFactoryBalanceAGradePcs()));
        // There is only one reject PCS field, so use it for both A and B Grade Rejects
        addFieldLabelValueRow("A Grade Reject (PCS)", String.valueOf(lot.getFactoryBalanceRejectPcs()));
        addFieldLabelValueRow("B Grade Quantity (PCS)", String.valueOf(lot.getFactoryBalanceBGradePcs()));
        addFieldLabelValueRow("B Grade Reject (PCS)", String.valueOf(lot.getFactoryBalanceRejectPcs()));
        // No separator after last section

        contentStream.close();
        // Draw the custom footer for single-lot PDF
        PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
        drawCustomFooter(cs, 1, 1);
        cs.close();
    }

    // Draws the logo and title at the top, centered as a group
    private void drawLogoAndTitleHeader(String title) throws IOException {
        // Load logo from resources (assets or drawable)
        Bitmap logoBitmap = null;
        try {
            // Try assets first
            InputStream is = context.getAssets().open("ic_logo.png");
            logoBitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            // Try drawable if not in assets
            int resId = context.getResources().getIdentifier("ic_logo", "drawable", context.getPackageName());
            if (resId != 0) {
                logoBitmap = BitmapFactory.decodeResource(context.getResources(), resId);
            }
        }
        if (logoBitmap == null) return;
        PDImageXObject logoImage = LosslessFactory.createFromImage(document, logoBitmap);
        float logoHeight = 40f;
        float logoWidth = logoBitmap.getWidth() * (logoHeight / logoBitmap.getHeight());
        float fontSize = 28f;
        PDType1Font font = PDType1Font.HELVETICA_BOLD;
        float textWidth = font.getStringWidth(title) / 1000 * fontSize;
        float gap = 18f;
        float groupWidth = logoWidth + gap + textWidth;
        float x = (PAGE_WIDTH - groupWidth) / 2;
        float y = currentY - logoHeight + 10;
        // Draw logo
        contentStream.drawImage(logoImage, x, y, logoWidth, logoHeight);
        // Draw title text, vertically centered with logo
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(64f/255, 73f/255, 78f/255); // dark gray
        contentStream.beginText();
        contentStream.newLineAtOffset(x + logoWidth + gap, y + (logoHeight - fontSize) / 2 + 6);
        contentStream.showText(title);
        contentStream.endText();
    }

    // Brand colors (from colors.xml)
    private static final float[] PRIMARY_COLOR = {64f / 255, 73f / 255, 78f / 255}; // #40494e
    private static final float[] ACCENT_COLOR = {3f / 255, 218f / 255, 197f / 255}; // #03DAC5
    private static final float[] SECTION_BG_COLOR = {237f / 255, 238f / 255, 243f / 255}; // #EDEEF3

    // Helper for section titles (bold, larger, colored, no underline)
    private void addSectionTitle(String title) throws IOException {
        currentY -= 4;
        float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 15;
        float x = (PAGE_WIDTH - textWidth) / 2;
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 15);
        contentStream.setNonStrokingColor(PRIMARY_COLOR[0], PRIMARY_COLOR[1], PRIMARY_COLOR[2]);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, currentY);
        contentStream.showText(title);
        contentStream.endText();
        currentY -= LINE_HEIGHT - 4;
    }

    // Helper for centered subtitle with NO underline (for Lot Details only)
    private void addCenteredTitleNoUnderline(String title, int fontSize) throws IOException {
        PDType1Font font = PDType1Font.HELVETICA_BOLD;
        float textWidth = font.getStringWidth(title) / 1000 * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2;
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(PRIMARY_COLOR[0], PRIMARY_COLOR[1], PRIMARY_COLOR[2]);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, currentY);
        contentStream.showText(title);
        contentStream.endText();
    }

    // Helper for field label + value (label bold, value normal, both left-aligned)
    private void addFieldLabelValue(String label, String value) throws IOException {
        float labelWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(label) / 1000 * 12;
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.setNonStrokingColor(PRIMARY_COLOR[0], PRIMARY_COLOR[1], PRIMARY_COLOR[2]);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText(label);
        contentStream.endText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.setNonStrokingColor(PRIMARY_COLOR[0], PRIMARY_COLOR[1], PRIMARY_COLOR[2]);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + labelWidth + 10, currentY);
        contentStream.showText(value);
        contentStream.endText();
        currentY -= LINE_HEIGHT;
    }

    // Helper to draw a subtle colored separator line
    private void drawSeparator() throws IOException {
        contentStream.setStrokingColor(212f/255, 212f/255, 212f/255); // #d4d4d4
        contentStream.moveTo(MARGIN, currentY);
        contentStream.lineTo(PAGE_WIDTH - MARGIN, currentY);
        contentStream.stroke();
        contentStream.setStrokingColor(0, 0, 0); // Reset to black
    }

    // Helper to format PCS (always show 0 PCS if zero)
    private String formatPcs(int pcs) {
        return pcs + " PCS";
    }

    // Section title: left-aligned, bold, #263238, larger, with margin
    private void addSectionTitleLeft(String title) throws IOException {
        currentY -= 2;
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
        contentStream.setNonStrokingColor(38f/255, 50f/255, 56f/255); // #263238
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText(title);
        contentStream.endText();
        currentY -= 22;
    }

    // Field label left, value right, both #263238, value bold
    private void addFieldLabelValueRow(String label, String value) throws IOException {
        float labelFontSize = 13f;
        float valueFontSize = 13f;
        float y = currentY;
        // Label (left)
        contentStream.setFont(PDType1Font.HELVETICA, labelFontSize);
        contentStream.setNonStrokingColor(38f/255, 50f/255, 56f/255); // #263238
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, y);
        contentStream.showText(label);
        contentStream.endText();
        // Value (right, bold)
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, valueFontSize);
        float valueWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(value) / 1000 * valueFontSize;
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_WIDTH - MARGIN - valueWidth, y);
        contentStream.showText(value);
        contentStream.endText();
        currentY -= 20;
    }

    // Custom separator: subtle #e0e0e0, full width
    private void drawCustomSeparator(float y) throws IOException {
        contentStream.setStrokingColor(224f/255, 224f/255, 224f/255); // #e0e0e0
        contentStream.moveTo(MARGIN, y);
        contentStream.lineTo(PAGE_WIDTH - MARGIN, y);
        contentStream.stroke();
        contentStream.setStrokingColor(0, 0, 0); // Reset to black
    }

    // Custom footer: matches provided image with signature image, developer credit, and line
    private void drawCustomFooter(PDPageContentStream cs, int pageNum, int totalPages) throws IOException {
        // 1. Page number (centered, top of footer area) in '1 of 10' format
        String pageText = pageNum + " of " + totalPages;
        PDType1Font font = PDType1Font.HELVETICA;
        int fontSize = 14;
        float textWidth = font.getStringWidth(pageText) / 1000 * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2;
        float y = 90; // Top of footer area
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(38f/255, 50f/255, 56f/255); // #263238
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(pageText);
        cs.endText();

        // 2. Horizontal line (below page number)
        float lineY = y - 22;
        cs.setStrokingColor(224f/255, 224f/255, 224f/255); // #e0e0e0
        cs.moveTo(MARGIN, lineY);
        cs.lineTo(PAGE_WIDTH - MARGIN, lineY);
        cs.stroke();
        cs.setStrokingColor(0, 0, 0); // Reset to black

        // 3. Signature image (centered below line)
        android.graphics.Bitmap signatureBitmap = null;
        // Try loading from assets first
        try {
            java.io.InputStream is = context.getAssets().open("signature.png");
            signatureBitmap = android.graphics.BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            signatureBitmap = null;
        }
        // If not found in assets, try drawable
        if (signatureBitmap == null) {
            int sigResId = context.getResources().getIdentifier("signature", "drawable", context.getPackageName());
            if (sigResId != 0) {
                signatureBitmap = android.graphics.BitmapFactory.decodeResource(context.getResources(), sigResId);
            }
        }
        if (signatureBitmap != null) {
            com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject sigImage = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, signatureBitmap);
            float sigWidth = 90f;
            float sigHeight = signatureBitmap.getHeight() * (sigWidth / signatureBitmap.getWidth());
            float sigX = (PAGE_WIDTH - sigWidth) / 2;
            float sigY = lineY - sigHeight - 8;
            cs.drawImage(sigImage, sigX, sigY, sigWidth, sigHeight);
            // 4. Developer credit (centered, below signature image)
            String credit = "Developed by Mushfiquzzaman Liyon";
            int creditFontSize = 16;
            float creditWidth = font.getStringWidth(credit) / 1000 * creditFontSize;
            float creditX = (PAGE_WIDTH - creditWidth) / 2;
            float creditY = sigY - 18;
            cs.setFont(font, creditFontSize);
            cs.beginText();
            cs.newLineAtOffset(creditX, creditY);
            cs.showText(credit);
            cs.endText();
        }
    }
} 