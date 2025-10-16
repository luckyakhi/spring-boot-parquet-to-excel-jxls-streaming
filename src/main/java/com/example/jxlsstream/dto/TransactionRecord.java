package com.example.jxlsstream.dto;

import java.time.LocalDate;

public record TransactionRecord(
    String accountId,
    String currency,
    double amount,
    LocalDate txnDate
) {}
