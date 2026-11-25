-- US-34: the organizer closes a dispute either by approving the AI suggestion or by entering the final
-- score manually; "матч закривається з рахунком 13:9". The final score and the decision source are
-- recorded on the dispute (see docs/contracts/data-schema.md §3, disputes).

ALTER TABLE disputes
    ADD COLUMN resolution_team1_score INTEGER NULL,
    ADD COLUMN resolution_team2_score INTEGER NULL,
    ADD COLUMN resolution_source VARCHAR(32) NULL;

ALTER TABLE disputes
    ADD CONSTRAINT chk_disputes_resolution_scores CHECK (
        (resolution_team1_score IS NULL OR resolution_team1_score >= 0) AND
        (resolution_team2_score IS NULL OR resolution_team2_score >= 0)
    ),
    ADD CONSTRAINT chk_disputes_resolution_source CHECK (
        resolution_source IS NULL OR resolution_source IN ('AI_SUGGESTION', 'MANUAL_SCORE')
    );
