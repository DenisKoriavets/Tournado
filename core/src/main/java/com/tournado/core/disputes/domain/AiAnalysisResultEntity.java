package com.tournado.core.disputes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Advisory AI verdict for a dispute (US-32, US-33). Never changes a match by itself (NFR-20).
 * {@code nicknames_detected} stores the per-nickname roster comparison; {@code raw_response_payload} the
 * untouched AI service answer for audit.
 */
@Entity
@Table(name = "ai_analysis_results")
public class AiAnalysisResultEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "dispute_id", nullable = false, updatable = false, unique = true)
    private UUID disputeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private AiAnalysisStatus status;

    @Column(name = "recognized_team1_score")
    private Integer recognizedTeam1Score;

    @Column(name = "recognized_team2_score")
    private Integer recognizedTeam2Score;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nicknames_detected", columnDefinition = "jsonb")
    private List<StoredNicknameCheck> nicknameChecks;

    @Column(name = "nicknames_matched_all", nullable = false)
    private boolean nicknamesMatchedAll;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response_payload", columnDefinition = "jsonb")
    private Map<String, Object> rawResponsePayload;

    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private Instant analyzedAt;

    public AiAnalysisResultEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (analyzedAt == null) {
            analyzedAt = Instant.now();
        }
    }

    public boolean hasUsableSuggestion() {
        return status == AiAnalysisStatus.SUCCESS && recognizedTeam1Score != null && recognizedTeam2Score != null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDisputeId() {
        return disputeId;
    }

    public void setDisputeId(UUID disputeId) {
        this.disputeId = disputeId;
    }

    public AiAnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AiAnalysisStatus status) {
        this.status = status;
    }

    public Integer getRecognizedTeam1Score() {
        return recognizedTeam1Score;
    }

    public void setRecognizedTeam1Score(Integer recognizedTeam1Score) {
        this.recognizedTeam1Score = recognizedTeam1Score;
    }

    public Integer getRecognizedTeam2Score() {
        return recognizedTeam2Score;
    }

    public void setRecognizedTeam2Score(Integer recognizedTeam2Score) {
        this.recognizedTeam2Score = recognizedTeam2Score;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public List<StoredNicknameCheck> getNicknameChecks() {
        return nicknameChecks;
    }

    public void setNicknameChecks(List<StoredNicknameCheck> nicknameChecks) {
        this.nicknameChecks = nicknameChecks;
    }

    public boolean isNicknamesMatchedAll() {
        return nicknamesMatchedAll;
    }

    public void setNicknamesMatchedAll(boolean nicknamesMatchedAll) {
        this.nicknamesMatchedAll = nicknamesMatchedAll;
    }

    public Map<String, Object> getRawResponsePayload() {
        return rawResponsePayload;
    }

    public void setRawResponsePayload(Map<String, Object> rawResponsePayload) {
        this.rawResponsePayload = rawResponsePayload;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Instant analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
