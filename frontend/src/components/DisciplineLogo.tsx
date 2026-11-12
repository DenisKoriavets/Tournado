import React from 'react'
import type { Discipline } from '../types/tournament'
import cs2Img from '../assets/disciplines/cs2.jpg'
import brawlStarsImg from '../assets/disciplines/brawlstars.jpg'
import chessImg from '../assets/disciplines/chess.png'

export const DISCIPLINE_LOGOS: Record<Discipline, string> = {
  CS2: cs2Img,
  BRAWL_STARS: brawlStarsImg,
  CHESS: chessImg
}

interface DisciplineLogoProps {
  discipline: Discipline
  className?: string
  alt?: string
}

export const DisciplineLogo: React.FC<DisciplineLogoProps> = ({
  discipline,
  className = 'w-5 h-5 rounded object-cover',
  alt
}) => {
  const src = DISCIPLINE_LOGOS[discipline]
  return (
    <img
      src={src}
      alt={alt || discipline}
      className={`inline-block shrink-0 ${className}`}
      loading="lazy"
    />
  )
}
