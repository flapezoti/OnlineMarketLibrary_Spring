package com.example.seller;
import com.example.common.events.InvoiceIssued;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvoiceIssuedDeserTest {

    private static final String RAW_JSON =  
    "{\"customer\":{\"CustomerId\":4,\"FirstName\":\"Delpha\",\"LastName\":\"Douglas\",\"Street\":\"Cyrusmouth\","
  + "\"Complement\":\"OConner Mount\",\"City\":\"Creek\",\"State\":\"Florida\",\"ZipCode\":\"72692\","
  + "\"PaymentType\":\"CREDIT_CARD\",\"CardNumber\":\"5113-3534-7804-5111\",\"CardHolderName\":\"Jakob Grant\","
  + "\"CardExpiration\":\"0525\",\"CardSecurityNumber\":\"596\",\"CardBrand\":\"VISA\",\"Installments\":5,"
  + "\"instanceId\":\"11\"},\"orderId\":1,\"invoiceNumber\":\"4-20250505-001\","
  + "\"issueDate\":\"2025-05-05T02:28:47.006221\",\"totalInvoice\":1048.96,"
  + "\"items\":[{\"orderId\":1,\"orderItemId\":1,\"productId\":7,\"productName\":\"Awesome Wooden Bike\","
  + "\"sellerId\":703,\"unitPrice\":12.8,\"shippingLimitDate\":\"2025-05-08T02:28:47.006221\","
  + "\"freightValue\":3.74,\"quantity\":2,\"totalItems\":25.6,\"totalAmount\":25.6,\"totalIncentive\":0.0}],"
  + "\"instanceId\":\"11\"}";

    @Test
    void shouldDeserialize() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        InvoiceIssued ii = mapper.readValue(RAW_JSON, InvoiceIssued.class);
        assertEquals("11", ii.getInstanceId());
    }
}



