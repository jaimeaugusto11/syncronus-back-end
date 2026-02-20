package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Entity
@Table(name = "rfq_question_responses")
@Data
@EqualsAndHashCode(callSuper = true)
public class RFQQuestionResponse extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private SupplierProposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private RFQQuestion question;

    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;

    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String evaluatorNotes;

    // Manual Getters and Setters
    public SupplierProposal getProposal() {
        return proposal;
    }

    public void setProposal(SupplierProposal proposal) {
        this.proposal = proposal;
    }

    public RFQQuestion getQuestion() {
        return question;
    }

    public void setQuestion(RFQQuestion question) {
        this.question = question;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getEvaluatorNotes() {
        return evaluatorNotes;
    }

    public void setEvaluatorNotes(String evaluatorNotes) {
        this.evaluatorNotes = evaluatorNotes;
    }
}
