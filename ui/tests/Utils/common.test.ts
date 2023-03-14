import { getHumanReadableDuration } from '@/components/utils'
import { TimeUnit } from '@/types'

test('The getHumanReadableDuration function', () => {
  expect(getHumanReadableDuration(undefined)).toBe('--')
  expect(getHumanReadableDuration(0)).toBe('0')
  expect(getHumanReadableDuration(10)).toBe('10ms')
  expect(getHumanReadableDuration(500)).toBe('500ms')
  expect(getHumanReadableDuration(1000)).toBe('1s')
  expect(getHumanReadableDuration(60000)).toBe('1m')
  expect(getHumanReadableDuration(62000)).toBe('1m2s')
  expect(getHumanReadableDuration(3603001)).toBe('1h3s')
  expect(getHumanReadableDuration(3663000)).toBe('1h1m3s')
  expect(getHumanReadableDuration(0, TimeUnit.Secs)).toBe('0')
  expect(getHumanReadableDuration(1, TimeUnit.Secs)).toBe('1s')
  expect(getHumanReadableDuration(59, TimeUnit.Secs)).toBe('59s')
  expect(getHumanReadableDuration(60, TimeUnit.Secs)).toBe('1m')
  expect(getHumanReadableDuration(61, TimeUnit.Secs)).toBe('1m1s')
  expect(getHumanReadableDuration(3600, TimeUnit.Secs)).toBe('1h')
  expect(getHumanReadableDuration(3661, TimeUnit.Secs)).toBe('1h1m1s')
  expect(getHumanReadableDuration(86461, TimeUnit.Secs)).toBe('1d1m1s')
  expect(getHumanReadableDuration(90061, TimeUnit.Secs)).toBe('1d1h1m1s')
})
