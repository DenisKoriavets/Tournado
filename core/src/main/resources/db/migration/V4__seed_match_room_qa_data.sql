-- Seeds a deterministic QA fixture per discipline (team, bracket, pending match) so the
-- Match Room / Ready Check / Map Veto slice (US-22, US-23, US-25, US-26) can be exercised
-- manually end-to-end before bracket generation (US-18..US-21) is implemented.

INSERT INTO users (id, email, display_name)
VALUES
    ('22222222-0001-4000-8000-000000000001', 'cs2.captain.a@test.tournado.ua', 'CyberHawk'),
    ('22222222-0001-4000-8000-000000000002', 'cs2.captain.b@test.tournado.ua', 'TitanCaptain'),
    ('22222222-0001-4000-8000-000000000003', 'brawl.captain.a@test.tournado.ua', 'ShellBlaster'),
    ('22222222-0001-4000-8000-000000000004', 'brawl.captain.b@test.tournado.ua', 'GemRunner'),
    ('22222222-0001-4000-8000-000000000005', 'chess.captain.a@test.tournado.ua', 'KnightOwl'),
    ('22222222-0001-4000-8000-000000000006', 'chess.captain.b@test.tournado.ua', 'RookMaster')
ON CONFLICT (id) DO NOTHING;

INSERT INTO teams (id, name, discipline, captain_id)
VALUES
    ('22222222-0002-4000-8000-000000000001', 'QA CyberHawks', 'CS2', '22222222-0001-4000-8000-000000000001'),
    ('22222222-0002-4000-8000-000000000002', 'QA Titan Phoenix', 'CS2', '22222222-0001-4000-8000-000000000002'),
    ('22222222-0002-4000-8000-000000000003', 'QA Shell Blasters', 'BRAWL_STARS', '22222222-0001-4000-8000-000000000003'),
    ('22222222-0002-4000-8000-000000000004', 'QA Gem Runners', 'BRAWL_STARS', '22222222-0001-4000-8000-000000000004'),
    ('22222222-0002-4000-8000-000000000005', 'QA Knight Owls', 'CHESS', '22222222-0001-4000-8000-000000000005'),
    ('22222222-0002-4000-8000-000000000006', 'QA Rook Masters', 'CHESS', '22222222-0001-4000-8000-000000000006')
ON CONFLICT (id) DO NOTHING;

INSERT INTO team_members (team_id, user_id)
VALUES
    ('22222222-0002-4000-8000-000000000001', '22222222-0001-4000-8000-000000000001'),
    ('22222222-0002-4000-8000-000000000002', '22222222-0001-4000-8000-000000000002'),
    ('22222222-0002-4000-8000-000000000003', '22222222-0001-4000-8000-000000000003'),
    ('22222222-0002-4000-8000-000000000004', '22222222-0001-4000-8000-000000000004'),
    ('22222222-0002-4000-8000-000000000005', '22222222-0001-4000-8000-000000000005'),
    ('22222222-0002-4000-8000-000000000006', '22222222-0001-4000-8000-000000000006')
ON CONFLICT (team_id, user_id) DO NOTHING;

INSERT INTO tournaments (
    id, name, description, discipline, format, slot_limit, status,
    registration_deadline_at, check_in_opens_at, start_time_at, cs2_server_connect, created_by_user_id
)
VALUES
    (
        '22222222-0003-4000-8000-000000000001',
        'Match Room QA Arena — CS2',
        'Детерміновані дані для ручного тестування Match Room, Ready Check та Map Veto.',
        'CS2', 'SINGLE_ELIMINATION', 8, 'IN_PROGRESS',
        CURRENT_TIMESTAMP - INTERVAL '3 hours',
        CURRENT_TIMESTAMP - INTERVAL '2 hours',
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        'connect 127.0.0.1:27015; password qa_cs2',
        '00000000-0000-0000-0000-000000000001'
    ),
    (
        '22222222-0003-4000-8000-000000000002',
        'Match Room QA Arena — Brawl Stars',
        'Детерміновані дані для ручного тестування Match Room, Ready Check та Map Veto.',
        'BRAWL_STARS', 'SINGLE_ELIMINATION', 8, 'IN_PROGRESS',
        CURRENT_TIMESTAMP - INTERVAL '3 hours',
        CURRENT_TIMESTAMP - INTERVAL '2 hours',
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        NULL,
        '00000000-0000-0000-0000-000000000001'
    ),
    (
        '22222222-0003-4000-8000-000000000003',
        'Match Room QA Arena — Chess.com',
        'Детерміновані дані для ручного тестування Match Room та авто-пропуску вето в шахах.',
        'CHESS', 'SINGLE_ELIMINATION', 8, 'IN_PROGRESS',
        CURRENT_TIMESTAMP - INTERVAL '3 hours',
        CURRENT_TIMESTAMP - INTERVAL '2 hours',
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        NULL,
        '00000000-0000-0000-0000-000000000001'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO tournament_map_pool (tournament_id, map_name, sort_order)
VALUES
    ('22222222-0003-4000-8000-000000000001', 'de_mirage', 0),
    ('22222222-0003-4000-8000-000000000001', 'de_inferno', 1),
    ('22222222-0003-4000-8000-000000000001', 'de_nuke', 2),
    ('22222222-0003-4000-8000-000000000001', 'de_ancient', 3),
    ('22222222-0003-4000-8000-000000000001', 'de_anubis', 4),
    ('22222222-0003-4000-8000-000000000001', 'de_dust2', 5),
    ('22222222-0003-4000-8000-000000000001', 'de_vertigo', 6),

    ('22222222-0003-4000-8000-000000000002', 'Shooting Star', 0),
    ('22222222-0003-4000-8000-000000000002', 'Backyard Bowl', 1),
    ('22222222-0003-4000-8000-000000000002', 'Hard Rock Mine', 2),
    ('22222222-0003-4000-8000-000000000002', 'Dueling Beetles', 3),
    ('22222222-0003-4000-8000-000000000002', 'Ring of Fire', 4)
ON CONFLICT (tournament_id, map_name) DO NOTHING;

INSERT INTO brackets (id, tournament_id, type, total_rounds)
VALUES
    ('22222222-0004-4000-8000-000000000001', '22222222-0003-4000-8000-000000000001', 'SINGLE_ELIMINATION', 3),
    ('22222222-0004-4000-8000-000000000002', '22222222-0003-4000-8000-000000000002', 'SINGLE_ELIMINATION', 3),
    ('22222222-0004-4000-8000-000000000003', '22222222-0003-4000-8000-000000000003', 'SINGLE_ELIMINATION', 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO matches (
    id, tournament_id, bracket_id, round_number, match_order, team1_id, team2_id, status, is_bye
)
VALUES
    (
        '22222222-0005-4000-8000-000000000001',
        '22222222-0003-4000-8000-000000000001',
        '22222222-0004-4000-8000-000000000001',
        1, 1,
        '22222222-0002-4000-8000-000000000001',
        '22222222-0002-4000-8000-000000000002',
        'PENDING', FALSE
    ),
    (
        '22222222-0005-4000-8000-000000000002',
        '22222222-0003-4000-8000-000000000002',
        '22222222-0004-4000-8000-000000000002',
        1, 1,
        '22222222-0002-4000-8000-000000000003',
        '22222222-0002-4000-8000-000000000004',
        'PENDING', FALSE
    ),
    (
        '22222222-0005-4000-8000-000000000003',
        '22222222-0003-4000-8000-000000000003',
        '22222222-0004-4000-8000-000000000003',
        1, 1,
        '22222222-0002-4000-8000-000000000005',
        '22222222-0002-4000-8000-000000000006',
        'PENDING', FALSE
    )
ON CONFLICT (id) DO NOTHING;
