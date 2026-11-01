import { describe, expect, it } from 'vitest'
import { ageOn, isAdult, isEmail, isEmployeeId, isIndianMobile, isStrongPassword, passwordChecks, strength } from './validation'

describe('password rules (same as the backend @StrongPassword)', () => {
  it('accepts the demo passwords', () => {
    expect(isStrongPassword('Passenger@123')).toBe(true)
    expect(isStrongPassword('Station@123')).toBe(true)
  })
  it('rejects a password missing any rule', () => {
    expect(isStrongPassword('Sh@1a')).toBe(false)
    expect(isStrongPassword('alllower@123')).toBe(false)
    expect(isStrongPassword('ALLUPPER@123')).toBe(false)
    expect(isStrongPassword('NoDigits@abc')).toBe(false)
    expect(isStrongPassword('NoSpecial123')).toBe(false)
  })
  it('scores strength as the number of rules met', () => {
    expect(strength('')).toBe(0)
    expect(strength('abc')).toBe(1)
    expect(strength('Passenger@123')).toBe(5)
    expect(passwordChecks('Abc').filter((c) => c.ok)).toHaveLength(2)
  })
})

describe('contact details', () => {
  it('validates Indian mobile numbers', () => {
    expect(isIndianMobile('9876543210')).toBe(true)
    expect(isIndianMobile('5876543210')).toBe(false)
    expect(isIndianMobile('987654321')).toBe(false)
  })
  it('validates emails and employee IDs', () => {
    expect(isEmail('passenger@payanam.in')).toBe(true)
    expect(isEmail('not-an-email')).toBe(false)
    expect(isEmployeeId('PJ-EMP-1001')).toBe(true)
    expect(isEmployeeId('PJ-1001')).toBe(false)
  })
})

describe('age check (must be 18+)', () => {
  const today = new Date('2026-10-18T00:00:00')
  it('counts whole years only', () => {
    expect(ageOn('2008-10-18', today)).toBe(18)
    expect(ageOn('2008-10-19', today)).toBe(17)
  })
  it('accepts adults and rejects minors and empty input', () => {
    expect(isAdult('1990-01-01', today)).toBe(true)
    expect(isAdult('2010-05-05', today)).toBe(false)
    expect(isAdult('', today)).toBe(false)
  })
})
