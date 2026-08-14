package com.travel.insurance.invoice;

import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import com.travel.insurance.invoice.dto.InvoiceItemResponse;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Component
public class InvoiceMapper {

    public Invoice toEntity(InvoiceRequest request) {
        Invoice invoice = new Invoice();
        updateEntity(invoice, request);
        return invoice;
    }

    public void updateEntity(Invoice invoice, InvoiceRequest request) {
        invoice.setClaimId(request.claimId());
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

    public InvoiceResponse toResponse(Invoice invoice, Function<UUID, String> medicalServiceNameResolver) {
        List<InvoiceItemResponse> items = invoice.getInvoiceItems().stream()
                .map(item -> {
                    String medicalServiceName = item.getMedicalServiceId() != null && medicalServiceNameResolver != null
                            ? medicalServiceNameResolver.apply(item.getMedicalServiceId())
                            : null;
                    return toItemResponse(item, medicalServiceName);
                })
                .toList();
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getClaimId(),
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getCurrency(),
                invoice.getTotalAmount(),
                invoice.getExchangeRate(),
                invoice.getBaseCurrency(),
                invoice.getBaseTotalAmount(),
                invoice.getFxRateDate(),
                items,
                invoice.getCreatedDate(),
                invoice.getUpdatedDate()
        );
    }

    private InvoiceItem toItem(InvoiceItemRequest request, Invoice invoice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setMedicalServiceId(request.medicalServiceId());
        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setAmount(request.amount());
        item.setServiceDate(request.serviceDate());
        return item;
    }

    private InvoiceItemResponse toItemResponse(InvoiceItem item, String medicalServiceName) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getMedicalServiceId(),
                medicalServiceName,
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getAmount(),
                item.getBaseUnitPrice(),
                item.getBaseAmount(),
                item.getServiceDate()
        );
    }
}
