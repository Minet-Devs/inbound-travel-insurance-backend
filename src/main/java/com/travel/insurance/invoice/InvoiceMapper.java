package com.travel.insurance.invoice;

import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import com.travel.insurance.invoice.dto.InvoiceItemResponse;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class InvoiceMapper {

    public Invoice toEntity(InvoiceRequest request) {
        Invoice invoice = new Invoice();
        updateEntity(invoice, request);
        return invoice;
    }

    public void updateEntity(Invoice invoice, InvoiceRequest request) {
        invoice.setClaimId(request.claimId());
        invoice.setMedicalServiceId(request.medicalServiceId());
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setIssueDate(request.issueDate());
        invoice.setCurrency(request.currency());
        invoice.setTotalAmount(request.totalAmount());
        List<InvoiceItem> items = Optional.ofNullable(request.invoiceItems()).orElse(List.of()).stream()
                .map(requestItem -> toItem(requestItem, invoice))
                .toList();
        invoice.getInvoiceItems().clear();
        invoice.getInvoiceItems().addAll(items);
    }

    public InvoiceResponse toResponse(Invoice invoice, String medicalServiceName) {
        List<InvoiceItemResponse> items = invoice.getInvoiceItems().stream()
                .map(this::toItemResponse)
                .toList();
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getClaimId(),
                invoice.getMedicalServiceId(),
                medicalServiceName,
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getCurrency(),
                invoice.getTotalAmount(),
                items,
                invoice.getCreatedDate(),
                invoice.getUpdatedDate()
        );
    }

    private InvoiceItem toItem(InvoiceItemRequest request, Invoice invoice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setAmount(request.amount());
        item.setServiceDate(request.serviceDate());
        return item;
    }

    private InvoiceItemResponse toItemResponse(InvoiceItem item) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getAmount(),
                item.getServiceDate()
        );
    }
}
