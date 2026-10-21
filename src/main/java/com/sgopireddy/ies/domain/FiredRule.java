package com.sgopireddy.ies.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

/** One entry in a determination's audit trail. */
@Embeddable
public class FiredRule {

    @Column(nullable = false) private String ruleId;
    @Column(nullable = false) private String status;
    @Column(nullable = false, length = 1000) private String reason;
    @Column(nullable = false) private String policyReference;

    protected FiredRule() {}

    public FiredRule(String ruleId, String status, String reason, String policyReference) {
        this.ruleId = ruleId;
        this.status = status;
        this.reason = reason;
        this.policyReference = policyReference;
    }

    public String getRuleId() { return ruleId; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getPolicyReference() { return policyReference; }
}
