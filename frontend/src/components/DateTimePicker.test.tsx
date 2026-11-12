import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { DateTimePicker } from './DateTimePicker'

describe('DateTimePicker component', () => {
  it('opens calendar dialog on calendar button click and applies selected date and time on Застосувати', () => {
    const handleChange = vi.fn()
    render(
      <DateTimePicker
        label="1. Дедлайн реєстрації"
        value="2026-11-15T18:00"
        onChange={handleChange}
      />
    )

    const openBtn = screen.getByRole('button', { name: /Обрати дату та час: 1\. Дедлайн реєстрації/i })
    fireEvent.click(openBtn)

    expect(screen.getByText(/Листопад 2026/i)).toBeInTheDocument()

    // Select day 20
    fireEvent.click(screen.getByRole('button', { name: '20' }))

    // Change hour and minute
    fireEvent.change(screen.getByLabelText('Години'), { target: { value: '19' } })
    fireEvent.change(screen.getByLabelText('Хвилини'), { target: { value: '30' } })

    // Confirm with Застосувати
    fireEvent.click(screen.getByRole('button', { name: 'Застосувати' }))

    expect(handleChange).toHaveBeenCalledWith('2026-11-20T19:30')
    expect(screen.queryByRole('button', { name: 'Застосувати' })).toBeNull()
  })

  it('cancels without committing changes when Скасувати is clicked', () => {
    const handleChange = vi.fn()
    render(
      <DateTimePicker
        label="3. Початок турніру"
        value="2026-11-15T18:00"
        onChange={handleChange}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: /Обрати дату та час: 3\. Початок турніру/i }))
    fireEvent.click(screen.getByRole('button', { name: '22' }))
    fireEvent.click(screen.getByRole('button', { name: 'Скасувати' }))

    expect(handleChange).not.toHaveBeenCalled()
    expect(screen.queryByRole('button', { name: 'Застосувати' })).toBeNull()
  })
})
