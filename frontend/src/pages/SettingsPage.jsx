import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import * as Label from '@radix-ui/react-label'
import { getTimeZones } from '@vvo/tzdb'
import { getPreferences, updatePreferences } from '@/api/preferences'

const DAY_KEYS = [
  { value: 'MONDAY', labelKey: 'monday' },
  { value: 'TUESDAY', labelKey: 'tuesday' },
  { value: 'WEDNESDAY', labelKey: 'wednesday' },
  { value: 'THURSDAY', labelKey: 'thursday' },
  { value: 'FRIDAY', labelKey: 'friday' },
  { value: 'SATURDAY', labelKey: 'saturday' },
  { value: 'SUNDAY', labelKey: 'sunday' },
]

const SESSION_DURATIONS = Array.from({ length: 16 }, (_, i) => (i + 1) * 15)

function generateTimeOptions(language) {
  const fmt = new Intl.DateTimeFormat(language, { hour: 'numeric', minute: '2-digit' })
  const options = []
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += 15) {
      const value = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`
      options.push({ value, label: fmt.format(new Date(2000, 0, 1, h, m)) })
    }
  }
  return options
}

const TIMEZONES = getTimeZones()

// Build a lookup from any IANA ID (including group members) to the canonical timezone entry
const TIMEZONE_BY_ID = new Map()
for (const tz of TIMEZONES) {
  TIMEZONE_BY_ID.set(tz.name, tz)
  for (const alias of tz.group) {
    if (!TIMEZONE_BY_ID.has(alias)) TIMEZONE_BY_ID.set(alias, tz)
  }
}

function formatTzLabel(tz) {
  const offset = tz.currentTimeFormat.split(' ')[0]
  return `(UTC${offset}) ${tz.alternativeName} — ${tz.mainCities.slice(0, 3).join(', ')}`
}

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
  const { t } = useTranslation('settings')
  const { data: prefs, isLoading } = useQuery({
    queryKey: ['preferences'],
    queryFn: getPreferences,
  })

  // Success/error live here so they survive the form remount after save
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState(null)

  if (isLoading || !prefs) {
    return (
      <div className="max-w-xl mx-auto px-6 py-10">
        <p className="text-ink-muted text-sm">{t('loadingPreferences')}</p>
      </div>
    )
  }

  // key-prop remount: when prefs change (e.g., after save), the form
  // remounts with fresh initial values — no useEffect + setState needed
  return (
    <SettingsForm
      key={JSON.stringify(prefs)}
      prefs={prefs}
      success={success}
      error={error}
      onSuccess={() => { setSuccess(true); setError(null); setTimeout(() => setSuccess(false), 3000) }}
      onError={(msg) => { setError(msg); setSuccess(false) }}
    />
  )
}

function SettingsForm({ prefs, success, error, onSuccess, onError }) {
  const { t, i18n } = useTranslation('settings')
  const queryClient = useQueryClient()
  const timeOptions = useMemo(() => generateTimeOptions(i18n.language), [i18n.language])
  const daysOfWeek = DAY_KEYS.map(d => ({ value: d.value, label: t(d.labelKey) }))
  const weekStartOptions = daysOfWeek

  const [form, setForm] = useState({
    displayName: prefs.displayName,
    timezone: prefs.timezone,
    defaultStartTime: prefs.defaultStartTime,
    defaultEndTime: prefs.defaultEndTime,
    defaultSessionMinutes: prefs.defaultSessionMinutes,
    weekStartDay: prefs.weekStartDay,
    ceremonyDay: prefs.ceremonyDay,
  })
  const [tzSearch, setTzSearch] = useState('')
  const [tzOpen, setTzOpen] = useState(false)

  const filteredTimezones = useMemo(() => {
    if (!tzSearch) return TIMEZONES
    const lower = tzSearch.toLowerCase()
    return TIMEZONES.filter(tz =>
      tz.alternativeName.toLowerCase().includes(lower) ||
      tz.mainCities.some(c => c.toLowerCase().includes(lower)) ||
      tz.countryName.toLowerCase().includes(lower) ||
      tz.name.toLowerCase().includes(lower) ||
      tz.group.some(g => g.toLowerCase().includes(lower))
    )
  }, [tzSearch])

  const mutation = useMutation({
    mutationFn: updatePreferences,
    onSuccess: (data) => {
      queryClient.setQueryData(['preferences'], data)
      onSuccess()
    },
    onError: (err) => {
      onError(err.message || t('failedToSave'))
    },
  })

  function handleSubmit(e) {
    e.preventDefault()

    const payload = {}
    if (form.displayName !== prefs.displayName) payload.displayName = form.displayName
    if (form.timezone !== prefs.timezone) payload.timezone = form.timezone
    if (form.defaultStartTime !== prefs.defaultStartTime) payload.defaultStartTime = form.defaultStartTime
    if (form.defaultEndTime !== prefs.defaultEndTime) payload.defaultEndTime = form.defaultEndTime
    if (form.defaultSessionMinutes !== prefs.defaultSessionMinutes) payload.defaultSessionMinutes = form.defaultSessionMinutes
    if (form.weekStartDay !== prefs.weekStartDay) payload.weekStartDay = form.weekStartDay
    if (form.ceremonyDay !== prefs.ceremonyDay) payload.ceremonyDay = form.ceremonyDay

    if (Object.keys(payload).length === 0) {
      onSuccess()
      return
    }

    mutation.mutate(payload)
  }

  function update(field, value) {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  return (
    <div className="max-w-xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-bold text-ink-heading mb-1">{t('settings')}</h1>
      <p className="text-sm text-ink-secondary mb-8">{t('subtitle')}</p>

      <form onSubmit={handleSubmit} className="space-y-10">
        {/* Profile */}
        <section>
          <h2 className="text-lg font-semibold text-ink-heading mb-4">{t('profile')}</h2>
          <div className="space-y-4">
            <Field label={t('preferredName')} htmlFor="displayName">
              <input
                id="displayName"
                type="text"
                value={form.displayName}
                onChange={e => update('displayName', e.target.value)}
                className={inputClass}
                placeholder={t('preferredNamePlaceholder')}
              />
            </Field>
          </div>
        </section>

        {/* Schedule */}
        <section>
          <h2 className="text-lg font-semibold text-ink-heading mb-4">{t('schedule')}</h2>
          <div className="space-y-4">
            <Field label={t('timezone')} htmlFor="timezone">
              <div className="relative">
                <input
                  id="timezone"
                  type="text"
                  value={tzOpen ? tzSearch : (TIMEZONE_BY_ID.get(form.timezone) ? formatTzLabel(TIMEZONE_BY_ID.get(form.timezone)) : form.timezone)}
                  onChange={e => { setTzSearch(e.target.value); setTzOpen(true) }}
                  onFocus={() => { setTzSearch(''); setTzOpen(true) }}
                  onBlur={() => setTimeout(() => setTzOpen(false), 200)}
                  className={inputClass}
                  placeholder={t('timezonePlaceholder')}
                />
                {tzOpen && (
                  <ul className="absolute z-10 w-full mt-1 max-h-60 overflow-y-auto bg-surface-raised border border-edge rounded-lg shadow-modal">
                    {filteredTimezones.map(tz => (
                      <li key={tz.name}>
                        <button
                          type="button"
                          className="w-full text-left px-3 py-2 text-sm text-ink-heading hover:bg-surface-soft"
                          onMouseDown={() => { update('timezone', tz.name); setTzOpen(false) }}
                        >
                          {formatTzLabel(tz)}
                        </button>
                      </li>
                    ))}
                    {filteredTimezones.length === 0 && (
                      <li className="px-3 py-2 text-sm text-ink-muted">{t('noMatches')}</li>
                    )}
                  </ul>
                )}
              </div>
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label={t('dayStartsAt')} htmlFor="defaultStartTime">
                <select
                  id="defaultStartTime"
                  value={form.defaultStartTime}
                  onChange={e => update('defaultStartTime', e.target.value)}
                  className={selectClass}
                >
                  {timeOptions.map(opt => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </Field>

              <Field label={t('dayEndsAt')} htmlFor="defaultEndTime">
                <select
                  id="defaultEndTime"
                  value={form.defaultEndTime}
                  onChange={e => update('defaultEndTime', e.target.value)}
                  className={selectClass}
                >
                  {timeOptions.map(opt => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </Field>
            </div>

            <Field label={t('defaultSessionLength')} htmlFor="defaultSessionMinutes">
              <select
                id="defaultSessionMinutes"
                value={form.defaultSessionMinutes}
                onChange={e => update('defaultSessionMinutes', Number(e.target.value))}
                className={selectClass}
              >
                {SESSION_DURATIONS.map(m => {
                  const hours = Math.floor(m / 60)
                  const mins = m % 60
                  let label
                  if (hours && mins) label = t('durationHoursMinutes', { hours, minutes: mins })
                  else if (hours) label = t('durationHours', { hours })
                  else label = t('durationMinutes', { minutes: mins })
                  return <option key={m} value={m}>{label}</option>
                })}
              </select>
            </Field>
          </div>
        </section>

        {/* Rituals */}
        <section>
          <h2 className="text-lg font-semibold text-ink-heading mb-4">{t('ritualsSection')}</h2>
          <div className="space-y-4">
            <Field label={t('weekStartsOn')} htmlFor="weekStartDay">
              <select
                id="weekStartDay"
                value={form.weekStartDay}
                onChange={e => update('weekStartDay', e.target.value)}
                className={selectClass}
              >
                {weekStartOptions.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </Field>

            <Field label={t('endOfWeekCeremonyDay')} htmlFor="ceremonyDay">
              <select
                id="ceremonyDay"
                value={form.ceremonyDay}
                onChange={e => update('ceremonyDay', e.target.value)}
                className={selectClass}
              >
                {daysOfWeek.map(opt => (
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
          <p className="text-sm text-emerald-600 font-medium" role="status">{t('preferencesSaved')}</p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="py-2.5 px-6 bg-primary-500 hover:bg-primary-600 disabled:bg-primary-400
            text-white text-sm font-semibold rounded-lg shadow-soft
            focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-2
            transition-colors duration-150"
        >
          {mutation.isPending ? t('common:saving') : t('common:save')}
        </button>
      </form>
    </div>
  )
}
