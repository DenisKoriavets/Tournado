import React from 'react'
import type { OrganizerDisputesState } from '../hooks/useOrganizerDisputes'
import type { DisputeSummary } from '../types/disputes'
import { formatDateTime } from '../utils/date'
import { AlertCircleIcon, ShieldIcon } from './icons/Icons'

export interface TournamentDisputesTabProps {
  state: OrganizerDisputesState
  onOpenDispute: (disputeId: string) => void
}

/** US-31: organizer dashboard of disputes — «Спір» vs «Опрацьовано» and the AI summary per match. */
export const TournamentDisputesTab: React.FC<TournamentDisputesTabProps> = ({ state, onOpenDispute }) => {
  const { data, error } = state

  if (error && !data) {
    return (
      <div role="alert" className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200 flex items-start gap-2">
        <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
        <span>{error.detail}</span>
      </div>
    )
  }
  if (!data) {
    return <div className="h-24 rounded-xl bg-[#0B0E14] animate-pulse" />
  }

  return (
    <div className="space-y-4">
      <div
        className={`p-4 rounded-xl border text-xs flex items-center gap-2 ${
          data.openDisputesCount > 0
            ? 'bg-rose-500/10 border-rose-500/30 text-rose-200'
            : 'bg-[#0B0E14] border-[#1E2536] text-slate-400'
        }`}
        role="status"
      >
        <ShieldIcon className={`w-4 h-4 ${data.openDisputesCount > 0 ? 'text-rose-400' : 'text-slate-500'}`} />
        <span>
          {data.openDisputesCount > 0
            ? `Відкритих спорів: ${data.openDisputesCount}. Ці матчі чекають на ваше рішення.`
            : 'Відкритих спорів немає.'}
        </span>
      </div>

      {data.disputes.length === 0 ? (
        <p className="text-xs text-slate-500">Спорів у цьому турнірі ще не було.</p>
      ) : (
        <ul className="space-y-2">
          {data.disputes.map((dispute) => (
            <li key={dispute.disputeId}>
              <button
                type="button"
                onClick={() => onOpenDispute(dispute.disputeId)}
                className={`w-full text-left p-3.5 rounded-xl border transition cursor-pointer space-y-1.5 ${
                  dispute.status === 'OPEN'
                    ? 'bg-rose-500/5 border-rose-500/40 hover:border-rose-400'
                    : 'bg-[#0B0E14] border-[#1E2536] hover:border-slate-600'
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-bold text-white">
                    Раунд {dispute.roundNumber} · Матч {dispute.matchOrder}: {dispute.team1.name ?? 'Команда'} vs{' '}
                    {dispute.team2.name ?? 'Команда'}
                  </span>
                  <span
                    className={`shrink-0 px-2 py-0.5 rounded-md border text-[10px] font-bold ${
                      dispute.status === 'OPEN'
                        ? 'bg-rose-500/15 text-rose-300 border-rose-500/40'
                        : 'bg-slate-800 text-slate-400 border-slate-700'
                    }`}
                  >
                    {dispute.status === 'OPEN' ? 'Спір' : 'Опрацьовано'}
                  </span>
                </div>
                <div className="text-[11px] text-slate-400 flex flex-wrap gap-x-3 gap-y-1">
                  <span>{aiSummary(dispute)}</span>
                  {dispute.aiAnalysisStatus === 'SUCCESS' && dispute.allRosterNicknamesFound === false && (
                    <span className="text-amber-300">Перевірте нікнейми</span>
                  )}
                  <span className="text-slate-500">відкрито {formatDateTime(dispute.createdAt)}</span>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function aiSummary(dispute: DisputeSummary): string {
  switch (dispute.aiAnalysisStatus) {
    case 'PENDING':
      return 'ШІ: аналіз триває…'
    case 'SUCCESS': {
      const score =
        dispute.aiRecognizedScoreTeam1 !== null && dispute.aiRecognizedScoreTeam2 !== null
          ? `${dispute.aiRecognizedScoreTeam1}:${dispute.aiRecognizedScoreTeam2}`
          : '—'
      const confidence = dispute.aiConfidence !== null ? ` (${Math.round(dispute.aiConfidence * 100)}%)` : ''
      return `ШІ: ${score}${confidence}`
    }
    default:
      return 'ШІ: рахунок не розпізнано'
  }
}
