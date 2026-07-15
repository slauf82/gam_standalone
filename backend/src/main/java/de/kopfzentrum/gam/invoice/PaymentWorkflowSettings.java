package de.kopfzentrum.gam.invoice;

import java.math.BigDecimal;

public record PaymentWorkflowSettings(
 boolean partialPaymentsEnabled, int defaultInstallmentCount, int installmentIntervalDays, BigDecimal minimumInstallmentAmount,
 int paymentTermDays, boolean reminderEnabled, int reminderDaysAfterDue,
 boolean dunning1Enabled, int dunning1DaysAfterReminder, BigDecimal dunning1Fee,
 boolean dunning2Enabled, int dunning2DaysAfterDunning1, BigDecimal dunning2Fee,
 boolean dunning3Enabled, int dunning3DaysAfterDunning2, BigDecimal dunning3Fee,
 boolean collectionEnabled, int collectionDaysAfterDunning3,
 BigDecimal annualInterestPercent, boolean automaticDocumentCreation, boolean manualApprovalRequired,
 boolean publishToPortal, boolean portalReadAloudEnabled, boolean automaticEmailDispatch
) {
 public static PaymentWorkflowSettings defaults(){return new PaymentWorkflowSettings(true,1,30,new BigDecimal("0.00"),14,true,7,true,14,new BigDecimal("0.00"),true,14,new BigDecimal("0.00"),true,14,new BigDecimal("0.00"),true,21,new BigDecimal("0.00"),false,true,true,true,false);}
}
