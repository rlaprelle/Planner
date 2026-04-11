import { useState, useEffect, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as Label from '@radix-ui/react-label'
import { getPreferences, updatePreferences } from '@/api/preferences'

const DAYS_OF_WEEK = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
]

const WEEK_START_OPTIONS = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'SUNDAY', label: 'Sunday' },
]

const SESSION_DURATIONS = Array.from({ length: 16 }, (_, i) => (i + 1) * 15)

function generateTimeOptions() {
  const options = []
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += 15) {
      const value = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`
      const label = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
      options.push({ value, label })
    }
  }
  return options
}

const TIME_OPTIONS = generateTimeOptions()

const TIMEZONE_LIST = Intl.supportedValuesOf('timeZone')

const inputClass =
  'w-full rounded-lg border border-edge px-3 py-2 text-ink-heading text-sm shadow-soft focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus'

const selectClass =
  'w-full rounded-lg border border-edge px-3 py-2 text-ink-heading text-sm shadow-soft focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus bg-surface'

function Field({ label, htmlFor, children }) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label.Root htmlFor={htmlFor} className="text-sm font-medium text-ink-body">
        {label}
      </Label.Root>
      {children}
    </div>
  )
}

export function SettingsPage() {
  const queryClient = useQueryClient()
  const { data: prefs, isLoading } = useQuery({
    queryKey: ['preferences'],
    queryFn: getPreferences,
  })

  const [form, setForm] = useState(null)
  const [tzSearch, setTzSearch] = useState('')
  const [tzOpen, setTzOpen] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (prefs && !form) {
      setForm({
        displayName: prefs.displayName,
        timezone: prefs.timezone,
        defaultStartTime: prefs.defaultStartTime,
        defaultEndTime: prefs.defaultEndTime,
        defaultSessionMinutes: prefs.defaultSessionMinutes,
        weekStartDay: prefs.weekStartDay,
        ceremonyDay: prefs.ceremonyDay,
      })
    }
  }, [prefs, form])

  const filteredTimezones = useMemo(() => {
    if (!tzSearch) return TIMEZONE_LIST.slice(0, 20)
    const lower = tzSearch.toLowerCase()
    return TIMEZONE_LIST.filter(tz => tz.toLowerCase().includes(lower)).slice(0, 20)
  }, [tzSearch])

  const mutation = useMutation({
    mutationFn: updatePreferences,
    onSuccess: (data) => {
      queryClient.setQueryData(['preferences'], data)
      setForm(null) // reset so useEffect re-syncs from updated prefs
      setSuccess(true)
      setError(null)
      setTimeout(() => setSuccess(false), 3000)
    },
    onError: (err) => {
      setError(err.message || 'Failed to save preferences')
      setSuccess(false)
    },
  })

  function handleSubmit(e) {
    e.preventDefault()
    setSuccess(false)
    setError(null)

    const payload = {}
    if (form.displayName !== prefs.displayName) payload.displayName = form.displayName
    if (form.timezone !== prefs.timezone) payload.timezone = form.timezone
    if (form.defaultStartTime !== prefs.defaultStartTime) payload.defaultStartTime = form.defaultStartTime
    if (form.defaultEndTime !== prefs.defaultEndTime) payload.defaultEndTime = form.defaultEndTime
    if (form.defaultSessionMinutes !== prefs.defaultSessionMinutes) payload.defaultSessionMinutes = form.defaultSessionMinutes
    if (form.weekStartDay !== prefs.weekStartDay) payload.weekStartDay = form.weekStartDay
    if (form.ceremonyDay !== prefs.ceremonyDay) payload.ceremonyDay = form.ceremonyDay

    if (Object.keys(payload).length === 0) {
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
      return
    }

    mutation.mutate(payload)
  }

  function update(field, value) {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  if (isLoading || !form) {
    return (
      <div className="max-w-xl mx-auto px-6 py-10">
        <p className="text-ink-muted text-sm">Loading preferences...</p>
      </div>
    )
  }

  return (
    <div className="max-w-xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-bold text-ink-heading mb-1">Settings</h1>
      <p className="text-sm text-ink-secondary mb-8">Customize how Planner works for you</p>

      <form onSubmit={handleSubmit} className="space-y-10">
        {/* Profile */}
        <section>
          <h2 className="text-lg font-semibold text-ink-heading mb-4">Profile</h2>
          <div className="space-y-4">
            <Field label="Preferred name" htmlFor="displayName">
              <input
                id="displayName"
                type="text"
                value={form.displayName}
                onChange={e => update('displayName', e.target.value)}
                className={inputClass}
                placeholder="What should we call you?"
              />
            </Field>
          </div>
        </section>

        {/* Schedule */}
        <section>
          <h2 className="text-lg font-semibold text-ink-heading mb-4">Schedule</h2>
          <div className="space-y-4">
            <Field label="Timezone" htmlFor="timezone">
              <div className="relative">
                <input
                  id="timezone"
                  type="text"
                  value={tzOpen ? tzSearch : form.timezone}
                  onChange={e => { setTzSearch(e.target.value); setTzOpen(true) }}
                  onFocus={() => { setTzSearch(''); setTzOpen(true) }}
                  onBlur={() => setTimeout(() => setTzOpen(false), 200)}
                  className={inputClass}
                  placeholder="Search timezones..."
                />
                {tzOpen && (
                  <ul className="absolute z-10 w-full mt-1 max-h-48 overflow-y-auto bg-surface-raised border border-edge rounded-lg shadow-modal">
                    {filteredTimezones.map(tz => (
                      <li key={tz}>
                        <button
                          type="button"
                          className="w-full text-left px-3 py-1.5 text-sm text-ink-heading hover:bg-surface-soft"
                          onMouseDown={() => { update('timezone', tz); setTzOpen(false) }}
                        >
                          {tz}
                        </button>
                      </li>
                    ))}
                    {filteredTimezones.length === 0 && (
                      <li className="px-3 py-2 text-sm text-ink-muted">No matches</li>
                    )}
                  </ul>
                )}
              </div>
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Day starts at" htmlFor="defaultStartTime">
                <select
                  id="defaultStartTime"
                  value={form.defaultStartTime}
                  onChange={e => update('defaultStartTime', e.target.value)}
                  className={selectClass}
                >
                  {TIME_OPTIONS.map(opt => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </Field>

              <Field label="Day ends at" htmlFor="defaultEndTime">
                <select
                  id="defaultEndTime"
                  value={form.defaultEndTime}
                  onChange={e => update('defaultEndTime', e.target.value)}
                  className={selectClass}
                >
                  {TIME_OPTIONS.map(opt => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </Field>
            </div>

            <Field label="Default session length" htmlFor="defaultSessionMinutes">
              <select
                id="defaultSessionMinutes"
                value={form.defaultSessionMinutes}
                onChange={e => update('defaultSessionMinutes', Number(e.target.value))}
                className={selectClass}
              >
                {SESSION_DURATIONS.map(m => (
                  <option key={m} value={m}>
                    {m >= 60 ? `${Math.floor(m / 60)}h${m % 60 ? ` ${m % 60}m` : ''}` : `${m}m`}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </section>

        {/* Rituals */}
        <section>
          <h2 className="text-lg font-semibold text-ink-heading mb-4">Rituals</h2>
          <div className="space-y-4">
            <Field label="Week starts on" htmlFor="weekStartDay">
              <select
                id="weekStartDay"
                value={form.weekStartDay}
                onChange={e => update('weekStartDay', e.target.value)}
                className={selectClass}
              >
                {WEEK_START_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </Field>

            <Field label="End-of-week ceremony day" htmlFor="ceremonyDay">
              <select
                id="ceremonyDay"
                value={form.ceremonyDay}
                onChange={e => update('ceremonyDay', e.target.value)}
                className={selectClass}
              >
                {DAYS_OF_WEEK.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </Field>
          </div>
        </section>

        {/* Feedback and submit */}
        {error && (
          <p className="text-sm text-error font-medium" role="alert">{error}</p>
        )}
        {success && (
          <p className="text-sm text-emerald-600 font-medium" role="status">Preferences saved!</p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="py-2.5 px-6 bg-primary-500 hover:bg-primary-600 disabled:bg-primary-400
            text-white text-sm font-semibold rounded-lg shadow-soft
            focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-2
            transition-colors duration-150"
        >
          {mutation.isPending ? 'Saving...' : 'Save'}
        </button>
      </form>
    </div>
  )
}
