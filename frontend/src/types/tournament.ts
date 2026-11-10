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
  description: string
  icon: string
  defaultMaps: string[]
  theme: {
    accentColor: string
    badgeBg: string
    badgeText: string
    badgeBorder: string
    ringColor: string
  }
}

export const DISCIPLINE_CONFIG: Record<Discipline, DisciplineMeta> = {
  CS2: {
    discipline: 'CS2',
    name: 'Counter-Strike 2',
    shortName: 'CS2',
    teamSize: 5,
    formatLabel: '5v5',
    description: 'Змагальний тактичний шутер Valve (MR12, Overtime MR3 $10k)',
    icon: '🎯',
    defaultMaps: ['de_mirage', 'de_inferno', 'de_nuke', 'de_ancient', 'de_anubis', 'de_dust2', 'de_vertigo'],
    theme: {
      accentColor: 'amber',
      badgeBg: 'bg-amber-500/10',
      badgeText: 'text-amber-400',
      badgeBorder: 'border-amber-500/30',
      ringColor: 'focus:ring-amber-500'
    }
  },
  BRAWL_STARS: {
    discipline: 'BRAWL_STARS',
    name: 'Brawl Stars 3v3',
    shortName: 'Brawl Stars',
    teamSize: 3,
    formatLabel: '3v3',
    description: 'Мобільний командний екшн Supercell (Friendly Battle лобі)',
    icon: '⚡',
    defaultMaps: ['Shooting Star', 'Backyard Bowl', 'Hard Rock Mine', 'Dueling Beetles', 'Ring of Fire'],
    theme: {
      accentColor: 'cyan',
      badgeBg: 'bg-cyan-500/10',
      badgeText: 'text-cyan-400',
      badgeBorder: 'border-cyan-500/30',
      ringColor: 'focus:ring-cyan-500'
    }
  },
  CHESS: {
    discipline: 'CHESS',
    name: 'Chess.com (Rapid 10+0)',
    shortName: 'Chess.com',
    teamSize: 1,
    formatLabel: '1v1',
    description: 'Класичні шахи онлайн на платформі Chess.com (Rapid 10+0)',
    icon: '♟️',
    defaultMaps: [],
    theme: {
      accentColor: 'violet',
      badgeBg: 'bg-violet-500/10',
      badgeText: 'text-violet-400',
      badgeBorder: 'border-violet-500/30',
      ringColor: 'focus:ring-violet-500'
    }
  }
}

export const STATUS_CONFIG: Record<TournamentStatus, { label: string; badgeClass: string; dotClass: string }> = {
  REGISTRATION_OPEN: {
    label: 'Реєстрація відкрита',
    badgeClass: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
    dotClass: 'bg-emerald-400'
  },
  CHECK_IN_ACTIVE: {
    label: 'Check-in активний',
    badgeClass: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
    dotClass: 'bg-amber-400 animate-pulse'
  },
  IN_PROGRESS: {
    label: 'Турнір триває',
    badgeClass: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/30',
    dotClass: 'bg-indigo-400 animate-pulse'
  },
  COMPLETED: {
    label: 'Завершено',
    badgeClass: 'bg-slate-500/10 text-slate-400 border-slate-500/30',
    dotClass: 'bg-slate-400'
  },
  DRAFT: {
    label: 'Чернетка',
    badgeClass: 'bg-slate-800 text-slate-400 border-slate-700',
    dotClass: 'bg-slate-500'
  }
}
