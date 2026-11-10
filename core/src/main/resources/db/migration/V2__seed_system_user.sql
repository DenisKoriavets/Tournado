INSERT INTO users (id, email, display_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'system.organizer@tournado.kma.ua', 'Tournado System Organizer')
ON CONFLICT (id) DO NOTHING;
