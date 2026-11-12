-- Seed diverse realistic esports tournaments across CS2, Brawl Stars, and Chess.com
-- Uses deterministic UUIDs and relative timestamps so seeded data remains valid on any launch date.

INSERT INTO tournaments (
    id,
    name,
    description,
    discipline,
    format,
    slot_limit,
    status,
    registration_deadline_at,
    check_in_opens_at,
    start_time_at,
    cs2_server_connect,
    created_by_user_id
)
VALUES
    (
        '11111111-1111-4111-8111-111111111101',
        'KMA CS2 Winter Major 2026',
        'Головний студентський турнір факультету інформатики з Counter-Strike 2. Формат 5v5, MR12, овертайми MR3 10k, автоматичний мап-вето в Match Room.',
        'CS2',
        'SINGLE_ELIMINATION',
        16,
        'REGISTRATION_OPEN',
        CURRENT_TIMESTAMP + INTERVAL '18 hours',
        CURRENT_TIMESTAMP + INTERVAL '20 hours',
        CURRENT_TIMESTAMP + INTERVAL '21 hours',
        'connect 194.44.12.5:27015; password kma_major',
        '00000000-0000-0000-0000-000000000001'
    ),
    (
        '11111111-1111-4111-8111-111111111102',
        'Brawl Stars KMA Showdown',
        'Динамічний 3v3 турнір у режимі Friendly Battle. Карти Gem Grab, Brawl Ball та Knockout у форматі Best-of-3.',
        'BRAWL_STARS',
        'SINGLE_ELIMINATION',
        16,
        'REGISTRATION_OPEN',
        CURRENT_TIMESTAMP + INTERVAL '2 days',
        CURRENT_TIMESTAMP + INTERVAL '2 days 2 hours',
        CURRENT_TIMESTAMP + INTERVAL '2 days 3 hours',
        NULL,
        '00000000-0000-0000-0000-000000000001'
    ),
    (
        '11111111-1111-4111-8111-111111111103',
        'Kyiv-Mohyla Rapid Open 10+0',
        'Відкритий онлайн-турнір із швидких шахів на платформі Chess.com. Контроль часу 10+0, олімпійська система на виліт.',
        'CHESS',
        'SINGLE_ELIMINATION',
        32,
        'REGISTRATION_OPEN',
        CURRENT_TIMESTAMP + INTERVAL '3 days',
        CURRENT_TIMESTAMP + INTERVAL '3 days 1 hour',
        CURRENT_TIMESTAMP + INTERVAL '3 days 2 hours',
        NULL,
        '00000000-0000-0000-0000-000000000001'
    ),
    (
        '11111111-1111-4111-8111-111111111104',
        'CS2 Fast Cup #12',
        'Вечірній експрес-турнір на 8 команд. Капітани мають підтвердити готовність у вікні Check-in до початку матчів.',
        'CS2',
        'SINGLE_ELIMINATION',
        8,
        'CHECK_IN_ACTIVE',
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        CURRENT_TIMESTAMP - INTERVAL '15 minutes',
        CURRENT_TIMESTAMP + INTERVAL '45 minutes',
        'connect 194.44.12.8:27016; password fastcup',
        '00000000-0000-0000-0000-000000000001'
    ),
    (
        '11111111-1111-4111-8111-111111111105',
        'NaUKMA Brawl Night Series #4',
        'Вечірня серія поєдинків 3v3 для студентських команд. Фінальні матчі транслюються на кампус-каналі.',
        'BRAWL_STARS',
        'SINGLE_ELIMINATION',
        8,
        'IN_PROGRESS',
        CURRENT_TIMESTAMP - INTERVAL '3 hours',
        CURRENT_TIMESTAMP - INTERVAL '2 hours',
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        NULL,
        '00000000-0000-0000-0000-000000000001'
    ),
    (
        '11111111-1111-4111-8111-111111111106',
        'Autumn Chess.com Classic',
        'Завершений осінній кубок із рапід-шахів серед студентів та випускників Могилянки.',
        'CHESS',
        'SINGLE_ELIMINATION',
        16,
        'COMPLETED',
        CURRENT_TIMESTAMP - INTERVAL '7 days',
        CURRENT_TIMESTAMP - INTERVAL '6 days',
        CURRENT_TIMESTAMP - INTERVAL '5 days',
        NULL,
        '00000000-0000-0000-0000-000000000001'
    )
ON CONFLICT (id) DO NOTHING;

-- Populate map pools for seeded CS2 and Brawl Stars tournaments
INSERT INTO tournament_map_pool (tournament_id, map_name, sort_order)
VALUES
    ('11111111-1111-4111-8111-111111111101', 'de_mirage', 0),
    ('11111111-1111-4111-8111-111111111101', 'de_inferno', 1),
    ('11111111-1111-4111-8111-111111111101', 'de_nuke', 2),
    ('11111111-1111-4111-8111-111111111101', 'de_ancient', 3),
    ('11111111-1111-4111-8111-111111111101', 'de_anubis', 4),
    ('11111111-1111-4111-8111-111111111101', 'de_dust2', 5),
    ('11111111-1111-4111-8111-111111111101', 'de_vertigo', 6),

    ('11111111-1111-4111-8111-111111111102', 'Shooting Star', 0),
    ('11111111-1111-4111-8111-111111111102', 'Backyard Bowl', 1),
    ('11111111-1111-4111-8111-111111111102', 'Hard Rock Mine', 2),
    ('11111111-1111-4111-8111-111111111102', 'Dueling Beetles', 3),
    ('11111111-1111-4111-8111-111111111102', 'Ring of Fire', 4),

    ('11111111-1111-4111-8111-111111111104', 'de_mirage', 0),
    ('11111111-1111-4111-8111-111111111104', 'de_inferno', 1),
    ('11111111-1111-4111-8111-111111111104', 'de_nuke', 2),
    ('11111111-1111-4111-8111-111111111104', 'de_ancient', 3),
    ('11111111-1111-4111-8111-111111111104', 'de_anubis', 4),
    ('11111111-1111-4111-8111-111111111104', 'de_dust2', 5),
    ('11111111-1111-4111-8111-111111111104', 'de_vertigo', 6),

    ('11111111-1111-4111-8111-111111111105', 'Shooting Star', 0),
    ('11111111-1111-4111-8111-111111111105', 'Backyard Bowl', 1),
    ('11111111-1111-4111-8111-111111111105', 'Hard Rock Mine', 2),
    ('11111111-1111-4111-8111-111111111105', 'Dueling Beetles', 3),
    ('11111111-1111-4111-8111-111111111105', 'Ring of Fire', 4)
ON CONFLICT (tournament_id, map_name) DO NOTHING;
