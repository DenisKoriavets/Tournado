export type Discipline = 'CS2' | 'BRAWL_STARS' | 'CHESS'

export type TournamentStatus =
  | 'DRAFT'
  | 'REGISTRATION_OPEN'
  | 'CHECK_IN_ACTIVE'
  | 'IN_PROGRESS'
  | 'COMPLETED'

export type TournamentFormat = 'SINGLE_ELIMINATION'

export type SlotLimit = 8 | 16 | 32

export interface CreateTournamentPayload {
  name: string
  description?: string
  discipline: Discipline
  slotLimit: SlotLimit
  registrationDeadlineAt: string // ISO-8601 UTC
  checkInOpensAt: string // ISO-8601 UTC
  startTimeAt: string // ISO-8601 UTC
  cs2ServerConnect?: string
}

export interface Tournament {
  id: string
  publicSlug: string
  name: string
  description?: string
  discipline: Discipline
  teamSize: number
  format: TournamentFormat
  slotLimit: number
  status: TournamentStatus
  registrationDeadlineAt: string
  checkInOpensAt: string
  startTimeAt: string
  cs2ServerConnect?: string
  mapPool?: string[]
  registeredTeamsCount: number
  createdAt: string
}

export interface InvalidParam {
  name: string
  reason: string
}

export interface ProblemDetail {
  type?: string
  title: string
  status: number
  detail: string
  instance?: string
  code?: string
  timestamp?: string
  invalidParams?: InvalidParam[]
}

export interface DisciplineMeta {
  discipline: Discipline
  name: string
  shortName: string
  teamSize: number
  formatLabel: string
  category: string
  description: string
  defaultMaps: string[]
  theme: {
    accentColor: string
    badgeBg: string
    badgeText: string
    badgeBorder: string
    glowBorder: string
  }
}

export const DISCIPLINE_CONFIG: Record<Discipline, DisciplineMeta> = {
  CS2: {
    discipline: 'CS2',
    name: 'Counter-Strike 2',
    shortName: 'CS2',
    teamSize: 5,
    formatLabel: '5v5',
    category: 'Tactical Shooter',
    description: 'Змагальний тактичний шутер Valve (MR12, Overtime MR3 $10k)',
    defaultMaps: ['de_mirage', 'de_inferno', 'de_nuke', 'de_ancient', 'de_anubis', 'de_dust2', 'de_vertigo'],
    theme: {
      accentColor: '#F59E0B',
      badgeBg: 'bg-amber-500/10',
      badgeText: 'text-amber-400',
      badgeBorder: 'border-amber-500/30',
      glowBorder: 'hover:border-amber-500/50'
    }
  },
  BRAWL_STARS: {
    discipline: 'BRAWL_STARS',
    name: 'Brawl Stars',
    shortName: 'Brawl Stars',
    teamSize: 3,
    formatLabel: '3v3',
    category: 'Mobile Action',
    description: 'Мобільний командний екшн Supercell (Friendly Battle лобі)',
    defaultMaps: ['Shooting Star', 'Backyard Bowl', 'Hard Rock Mine', 'Dueling Beetles', 'Ring of Fire'],
    theme: {
      accentColor: '#A855F7',
      badgeBg: 'bg-purple-500/10',
      badgeText: 'text-purple-400',
      badgeBorder: 'border-purple-500/30',
      glowBorder: 'hover:border-purple-500/50'
    }
  },
  CHESS: {
    discipline: 'CHESS',
    name: 'Chess.com',
    shortName: 'Chess.com',
    teamSize: 1,
    formatLabel: '1v1',
    category: 'Rapid Strategy',
    description: 'Класичні шахи онлайн на платформі Chess.com (Rapid 10+0)',
    defaultMaps: [],
    theme: {
      accentColor: '#38BDF8',
      badgeBg: 'bg-sky-500/10',
      badgeText: 'text-sky-300',
      badgeBorder: 'border-sky-500/30',
      glowBorder: 'hover:border-sky-500/50'
    }
  }
}

export const STATUS_CONFIG: Record<TournamentStatus, { label: string; badgeClass: string; dotClass: string }> = {
  REGISTRATION_OPEN: {
    label: 'РЕЄСТРАЦІЯ',
    badgeClass: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
    dotClass: 'bg-emerald-400 animate-pulse'
  },
  CHECK_IN_ACTIVE: {
    label: 'CHECK-IN',
    badgeClass: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
    dotClass: 'bg-amber-400 animate-pulse'
  },
  IN_PROGRESS: {
    label: 'НАЖИВО',
    badgeClass: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
    dotClass: 'bg-rose-400 animate-ping'
  },
  COMPLETED: {
    label: 'ЗАВЕРШЕНО',
    badgeClass: 'bg-slate-500/10 text-slate-400 border-slate-500/30',
    dotClass: 'bg-slate-500'
  },
  DRAFT: {
    label: 'ЧЕРНЕТКА',
    badgeClass: 'bg-slate-800 text-slate-400 border-slate-700',
    dotClass: 'bg-slate-600'
  }
}
