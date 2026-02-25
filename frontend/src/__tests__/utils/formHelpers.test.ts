import { describe, it, expect } from 'vitest'
import { calculateAge, isValidPollenUid } from '@/utils/formHelpers'

describe('calculateAge', () => {
  it('should calculate age correctly for a past birthday this year', () => {
    const today = new Date()
    const birthYear = today.getFullYear() - 25
    const birthDate = `${birthYear}-01-01`
    expect(calculateAge(birthDate)).toBe(25)
  })

  it('should subtract 1 if birthday has not occurred yet this year', () => {
    const today = new Date()
    const birthYear = today.getFullYear() - 20
    // Use December 31 which is always in the future (unless today is Dec 31)
    const birthDate = `${birthYear}-12-31`
    const expected = today.getMonth() === 11 && today.getDate() >= 31 ? 20 : 19
    expect(calculateAge(birthDate)).toBe(expected)
  })

  it('should return 0 for a birth date this year', () => {
    const today = new Date()
    const month = String(today.getMonth() + 1).padStart(2, '0')
    const day = String(today.getDate()).padStart(2, '0')
    const birthDate = `${today.getFullYear()}-${month}-${day}`
    expect(calculateAge(birthDate)).toBe(0)
  })

  it('should handle age 18 boundary correctly', () => {
    const today = new Date()
    const birthYear = today.getFullYear() - 18
    const birthDate = `${birthYear}-01-01`
    expect(calculateAge(birthDate)).toBeGreaterThanOrEqual(18)
  })
})

describe('isValidPollenUid', () => {
  it('should accept valid QQ numbers (5-11 digits, first not 0)', () => {
    expect(isValidPollenUid('12345')).toBe(true)
    expect(isValidPollenUid('123456789')).toBe(true)
    expect(isValidPollenUid('12345678901')).toBe(true)
  })

  it('should reject QQ numbers starting with 0', () => {
    expect(isValidPollenUid('01234')).toBe(false)
    expect(isValidPollenUid('0123456789')).toBe(false)
  })

  it('should reject QQ numbers shorter than 5 digits', () => {
    expect(isValidPollenUid('1234')).toBe(false)
    expect(isValidPollenUid('1')).toBe(false)
  })

  it('should reject QQ numbers longer than 11 digits', () => {
    expect(isValidPollenUid('123456789012')).toBe(false)
  })

  it('should reject non-numeric input', () => {
    expect(isValidPollenUid('abcde')).toBe(false)
    expect(isValidPollenUid('1234a')).toBe(false)
    expect(isValidPollenUid('12 34 5')).toBe(false)
  })

  it('should reject empty string', () => {
    expect(isValidPollenUid('')).toBe(false)
  })
})
