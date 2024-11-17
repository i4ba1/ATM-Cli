package com.nizar.atm.repository;

import com.nizar.atm.model.Debt;

import java.util.List;
import java.util.UUID;

public interface DebtRepository {
    Debt save(Debt debt);
    List<Debt> findActiveDebtsByDebtorId(UUID debtorId);
    void updateDebtStatus(UUID id, String status);
}
