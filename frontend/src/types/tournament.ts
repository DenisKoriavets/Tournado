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
    topStripClass: string
    hoverShadowClass: string
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
    description: 'Змагальний тактичний шутер Valve (MR12, Overtime MR3 10k)',
    defaultMaps: ['de_mirage', 'de_inferno', 'de_nuke', 'de_ancient', 'de_anubis', 'de_dust2', 'de_vertigo'],
    theme: {
      accentColor: '#F97316',
      badgeBg: 'bg-orange-500/10',
      badgeText: 'text-orange-400',
      badgeBorder: 'border-orange-500/30',
      glowBorder: 'hover:border-orange-500/50',
      topStripClass: 'from-orange-500 via-amber-400 to-orange-600',
      hoverShadowClass: 'hover:shadow-orange-500/10'
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
      accentColor: '#FACC15',
      badgeBg: 'bg-yellow-500/10',
      badgeText: 'text-yellow-300',
      badgeBorder: 'border-yellow-500/30',
      glowBorder: 'hover:border-yellow-400/50',
      topStripClass: 'from-yellow-400 via-amber-400 to-yellow-500',
      hoverShadowClass: 'hover:shadow-yellow-500/10'
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
      accentColor: '#4ADE80',
      badgeBg: 'bg-emerald-500/10',
      badgeText: 'text-emerald-400',
      badgeBorder: 'border-emerald-500/30',
      glowBorder: 'hover:border-emerald-400/50',
      topStripClass: 'from-emerald-400 via-green-500 to-emerald-600',
      hoverShadowClass: 'hover:shadow-emerald-500/10'
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
