/**
 * Calculate age from a birth date string (YYYY-MM-DD) relative to the current date.
 * If the current month/day is before the birth month/day, subtract 1 from the year difference.
 */
export function calculateAge(birthDateStr: string): number {
  const today = new Date()
  const birth = new Date(birthDateStr)
  let age = today.getFullYear() - birth.getFullYear()
  const monthDiff = today.getMonth() - birth.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--
  }
  return age
}

/** Validate QQ number format: 5-11 digits, first digit not 0 */
export function isValidPollenUid(uid: string): boolean {
  return /^[1-9]\d{4,10}$/.test(uid)
}
