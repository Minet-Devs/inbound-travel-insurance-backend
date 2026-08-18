package com.travel.insurance.memberstatement;

import com.travel.insurance.memberstatement.dto.MemberStatementResponse;

import java.time.LocalDate;

public interface MemberStatementService {

    /** Full statement for the member: current benefit allocation/utilization/balance and every claim transaction. */
    MemberStatementResponse getStatement(String passportNumber);

    byte[] export(String passportNumber, LocalDate fromDate, LocalDate toDate,
                  MemberStatementExportType exportType);
}