package it.lsimpresa.manager.order;

import it.lsimpresa.manager.exception.ConflictException;
import it.lsimpresa.manager.exception.ResourceNotAccessExeption;
import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderExportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final OrderService orderService;

    public record ExportDocument(byte[] content, MediaType contentType, String filename) {}

    @Transactional(readOnly = true)
    public ExportDocument export(Long id, ExportScope scope, ExportFormat format, UserJpa actor, boolean admin) {
        OrderJpa order = orderService.findDetailed(id);
        assertCanExport(order, actor, admin);
        List<OrderDetailJpa> details = scope == ExportScope.purchase
                ? order.getDetails().stream().filter(detail -> detail.getStatus() == OrderDetailStatus.TO_PURCHASE).toList()
                : order.getDetails();
        if (scope == ExportScope.purchase && details.isEmpty()) {
            throw new ConflictException("Non ci sono righe da ordinare per questo ordine");
        }
        String basename = "ordine-" + order.getId() + "-" + (scope == ExportScope.purchase ? "da-ordinare" : "completo");
        return format == ExportFormat.csv
                ? new ExportDocument(csv(order, details, scope), MediaType.parseMediaType("text/csv;charset=UTF-8"), basename + ".csv")
                : new ExportDocument(pdf(order, details, scope), MediaType.APPLICATION_PDF, basename + ".pdf");
    }

    private void assertCanExport(OrderJpa order, UserJpa actor, boolean admin) {
        if (admin) return;
        if (actor == null || actor.getRole() != UserRole.WAREHOUSE_ROLE
                || order.getAssignedWarehouse() == null
                || !order.getAssignedWarehouse().getId().equals(actor.getId())) {
            throw new ResourceNotAccessExeption("Solo il magazziniere assegnato può esportare questo ordine");
        }
    }

    private byte[] csv(OrderJpa order, List<OrderDetailJpa> details, ExportScope scope) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        if (scope == ExportScope.purchase) {
            row(csv, "ID ordine", "Commessa", "Data", "SKU", "Descrizione", "Quantità da ordinare", "Prezzo unitario", "Subtotale residuo");
            details.forEach(detail -> row(csv, order.getId(), order.getName(), date(order), detail.displaySku(),
                    detail.displayDescription(), remaining(detail), money(detail.getUnitPrice()), money(residualSubtotal(detail))));
        } else {
            row(csv, "ID ordine", "Commessa", "Data", "Priorità", "Categoria", "Stato ordine", "Magazziniere", "Note", "Totale netto", "SKU", "Descrizione", "Stato riga", "Quantità richiesta", "Quantità prelevata", "Quantità residua", "Prezzo unitario", "Subtotale riga");
            details.forEach(detail -> row(csv, order.getId(), order.getName(), date(order), label(order.getPriority()), label(order.getCategory()),
                    label(order.getStatus()), warehouseName(order), order.getNotes(), money(orderTotal(order)), detail.displaySku(),
                    detail.displayDescription(), label(detail.getStatus()), detail.getQuantity(), detail.getPickedQuantity(), remaining(detail),
                    money(detail.getUnitPrice()), money(subtotal(detail))));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(OrderJpa order, List<OrderDetailJpa> details, ExportScope scope) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(document);
            writer.text("Ordine #" + order.getId() + " · " + safe(order.getName()), BOLD, 16);
            writer.text("Data: " + date(order) + "    Priorità: " + label(order.getPriority()) + "    Stato: " + label(order.getStatus()), REGULAR, 9);
            writer.text("Magazziniere: " + warehouseName(order), REGULAR, 9);
            if (order.getNotes() != null) writer.text("Note: " + order.getNotes(), REGULAR, 9);
            writer.gap(8);
            if (scope == ExportScope.purchase) {
                writer.tableHeader("SKU", "Descrizione", "Da ordinare", "Prezzo", "Subtotale");
                for (OrderDetailJpa detail : details) writer.tableRow(detail.displaySku(), detail.displayDescription(),
                        String.valueOf(remaining(detail)), money(detail.getUnitPrice()), money(residualSubtotal(detail)));
                writer.total("Totale da ordinare: " + money(details.stream().map(OrderExportService::residualSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add)));
            } else {
                writer.tableHeader("SKU", "Descrizione", "Stato", "Q.tà", "Prel.", "Residuo", "Prezzo", "Subtotale");
                for (OrderDetailJpa detail : details) writer.tableRow(detail.displaySku(), detail.displayDescription(), label(detail.getStatus()),
                        String.valueOf(detail.getQuantity()), String.valueOf(detail.getPickedQuantity()), String.valueOf(remaining(detail)),
                        money(detail.getUnitPrice()), money(subtotal(detail)));
                writer.total("Totale netto: " + money(orderTotal(order)));
            }
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Impossibile generare il PDF dell'ordine", exception);
        }
    }

    private static class PdfWriter {
        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;
        private final float margin = 36;
        PdfWriter(PDDocument document) throws IOException { this.document = document; page(); }
        void page() throws IOException { if (stream != null) stream.close(); document.addPage(new PDPage(PDRectangle.A4)); stream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages() - 1)); y = PDRectangle.A4.getHeight() - margin; }
        void text(String value, PDType1Font font, float size) throws IOException { ensure(18); stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(margin, y); stream.showText(clip(value, 110)); stream.endText(); y -= size + 5; }
        void gap(float size) { y -= size; }
        void tableHeader(String... cells) throws IOException { table(cells, BOLD); }
        void tableRow(String... cells) throws IOException { table(cells, REGULAR); }
        void table(String[] cells, PDType1Font font) throws IOException { ensure(18); float width = (PDRectangle.A4.getWidth() - margin * 2) / cells.length; for (int i = 0; i < cells.length; i++) { stream.beginText(); stream.setFont(font, 7); stream.newLineAtOffset(margin + i * width, y); stream.showText(clip(cells[i], Math.max(10, (int) (width / 4.1f)))); stream.endText(); } y -= 13; }
        void total(String value) throws IOException { y -= 6; text(value, BOLD, 10); }
        void ensure(float needed) throws IOException { if (y - needed < margin) page(); }
        void close() throws IOException { if (stream != null) { stream.close(); stream = null; } }
    }

    private static void row(StringBuilder target, Object... cells) { for (int index = 0; index < cells.length; index++) { if (index > 0) target.append(';'); String cell = cells[index] == null ? "" : String.valueOf(cells[index]); target.append('"').append(cell.replace("\"", "\"\"")).append('"'); } target.append("\r\n"); }
    private static int remaining(OrderDetailJpa detail) { return detail.getQuantity() - detail.getPickedQuantity(); }
    private static BigDecimal subtotal(OrderDetailJpa detail) { return detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())); }
    private static BigDecimal residualSubtotal(OrderDetailJpa detail) { return detail.getUnitPrice().multiply(BigDecimal.valueOf(remaining(detail))); }
    private static BigDecimal orderTotal(OrderJpa order) { return order.getDetails().stream().map(OrderExportService::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private static String money(BigDecimal value) { return "€ " + value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ','); }
    private static String date(OrderJpa order) { return order.getDate().format(DATE); }
    private static String warehouseName(OrderJpa order) { UserJpa user = order.getAssignedWarehouse(); if (user == null) return "Non assegnato"; String name = String.join(" ", user.getName() == null ? "" : user.getName(), user.getSurname() == null ? "" : user.getSurname()).trim(); return name.isBlank() ? user.getUsername() : name; }
    private static String label(Object value) { return value == null ? "" : switch (value.toString()) { case "PROCESSING" -> "In lavorazione"; case "COMPLETED" -> "Completato"; case "CANCELED" -> "Annullato"; case "TO_PICK" -> "Da prelevare"; case "TO_PURCHASE" -> "Da ordinare/acquistare"; case "REPLACED" -> "Sostituito"; case "LOW" -> "Bassa"; case "STANDARD" -> "Standard"; case "HIGH" -> "Alta"; default -> value.toString(); }; }
    private static String safe(String value) { return value == null ? "" : value.replaceAll("[\\r\\n]", " "); }
    private static String clip(String value, int length) { String normalized = safe(value); return normalized.length() <= length ? normalized : normalized.substring(0, Math.max(0, length - 3)) + "..."; }
}
