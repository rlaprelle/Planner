import { useState, useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import * as Dialog from '@radix-ui/react-dialog'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { createDeferredItem } from '@/api/deferred'
import { FlyAwayCard } from './FlyAwayCard'

function playChime() {
  try {
    const ctx = new AudioContext()
    const t = ctx.currentTime
    const duration = 3.0

    // Fundamental ~280 Hz — deep bowl tone
    const osc1 = ctx.createOscillator()
    const gain1 = ctx.createGain()
    osc1.connect(gain1)
    gain1.connect(ctx.destination)
    osc1.frequency.value = 280
    osc1.type = 'sine'
    gain1.gain.setValueAtTime(0, t)
    gain1.gain.linearRampToValueAtTime(0.35, t + 0.015)   // sharp strike
    gain1.gain.exponentialRampToValueAtTime(0.001, t + duration)
    osc1.start(t)
    osc1.stop(t + duration)

    // Upper partial ~770 Hz (2.75× fundamental — inharmonic, typical of metal bowls)
    const osc2 = ctx.createOscillator()
    const gain2 = ctx.createGain()
    osc2.connect(gain2)
    gain2.connect(ctx.destination)
    osc2.frequency.value = 280 * 2.756
    osc2.type = 'sine'
    gain2.gain.setValueAtTime(0, t)
    gain2.gain.linearRampToValueAtTime(0.12, t + 0.015)
    gain2.gain.exponentialRampToValueAtTime(0.001, t + duration * 0.6)
    osc2.start(t)
    osc2.stop(t + duration)

    osc1.onended = () => ctx.close()
  } catch {
    // AudioContext unavailable (e.g. in test environments) — silently ignore
  }
}

export function QuickCapture() {
  const { t } = useTranslation('deferred')
  const [open, setOpen] = useState(false)
  const [text, setText] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const [error, setError] = useState(null)
  const [brainDumpFlash, setBrainDumpFlash] = useState(false)
  const [brainDumpPending, setBrainDumpPending] = useState(false)
  const [flyAwayRects, setFlyAwayRects] = useState(null)

  const queryClient = useQueryClient()
  const timeoutRef = useRef(null)
  const textareaRef = useRef(null)
  const brainDumpTimeoutRef = useRef(null)

  function launchFlyAwayCard() {
    const start = textareaRef.current?.getBoundingClientRect()
    const inboxEl = document.querySelector('a[href="/inbox"]')
    const end = inboxEl?.getBoundingClientRect()
    if (start && end) setFlyAwayRects({ start, end })
  }

  const mutation = useMutation({
    mutationFn: () => createDeferredItem(text.trim()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['deferred'] })
      playChime()
      launchFlyAwayCard()
      setConfirmed(true)
      timeoutRef.current = setTimeout(() => {
        setOpen(false)
        setConfirmed(false)
        setText('')
        setError(null)
      }, 1000)
    },
    onError: () => {
      setError(t('couldntSave'))
    },
  })

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current)
      if (brainDumpTimeoutRef.current) clearTimeout(brainDumpTimeoutRef.current)
    }
  }, [])

  // Ctrl+Space global hotkey — toggles modal open/closed
  useEffect(() => {
    function handleKeyDown(e) {
      if (e.ctrlKey && e.code === 'Space') {
        e.preventDefault()
        setOpen(prev => !prev)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  function handleOpenChange(next) {
    if (!next) {
      setOpen(false)
      setText('')
      setConfirmed(false)
      setError(null)
      setBrainDumpFlash(false)
      setBrainDumpPending(false)
      setFlyAwayRects(null)
      if (brainDumpTimeoutRef.current) {
        clearTimeout(brainDumpTimeoutRef.current)
        brainDumpTimeoutRef.current = null
      }
    } else {
      setOpen(true)
    }
  }

  function handleBrainDumpSubmit() {
    if (text.trim() && !mutation.isPending && !brainDumpPending) {
      const trimmed = text.trim()
      setBrainDumpPending(true)
      createDeferredItem(trimmed).then(() => {
        queryClient.invalidateQueries({ queryKey: ['deferred'] })
        playChime()
        launchFlyAwayCard()
        setBrainDumpFlash(true)
        setBrainDumpPending(false)
        setText('')
        setError(null)
        // Refocus immediately — toast is at the bottom of the screen,
        // so the textarea is available right away
        setTimeout(() => textareaRef.current?.focus(), 0)
        // Remove toast from DOM after full animation (1860ms)
        brainDumpTimeoutRef.current = setTimeout(() => {
          setBrainDumpFlash(false)
          brainDumpTimeoutRef.current = null
        }, 1860)
      }).catch(() => {
        setBrainDumpPending(false)
        setError(t('couldntSave'))
      })
    }
  }

  function handleTextareaKeyDown(e) {
    if (e.key === 'Enter' && e.ctrlKey && !e.shiftKey) {
      e.preventDefault()
      handleBrainDumpSubmit()
    } else if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey) {
      e.preventDefault()
      if (text.trim() && !mutation.isPending) mutation.mutate()
    }
  }

  const isBlank = !text.trim()

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Trigger asChild>
        <button
          className="w-full text-left px-3 py-2 rounded-md text-sm font-medium text-primary-500 hover:bg-primary-50 hover:text-primary-700 transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1"
        >
          {t('quickCaptureButton')}
        </button>
      </Dialog.Trigger>

      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/30 z-40" />
        <Dialog.Content
          className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-md bg-surface-raised rounded-xl shadow-modal p-6 focus:outline-none"
          aria-label={t('quickCapture')}
        >
          {confirmed ? (
            <div className="flex flex-col items-center gap-3 py-4">
              <svg
                className="text-success w-10 h-10"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2}
                aria-hidden="true"
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
              <p className="text-ink-body font-medium">{t('captured')}</p>
            </div>
          ) : (
            <div className="relative">
              <Dialog.Title className="text-base font-semibold text-ink-heading mb-3">
                {t('quickCapture')}
              </Dialog.Title>
              <Dialog.Description className="sr-only">
                {t('quickCaptureHint')}
              </Dialog.Description>

              <textarea
                ref={textareaRef}
                autoFocus
                rows={3}
                className="w-full rounded-md border border-edge px-3 py-2 text-sm text-ink-heading placeholder-ink-muted resize-none focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus"
                placeholder={t('whatsOnYourMind')}
                value={text}
                onChange={e => { setText(e.target.value); setError(null) }}
                onKeyDown={handleTextareaKeyDown}
              />

              {error && (
                <p className="mt-1 text-xs text-error" role="alert">{error}</p>
              )}

              {brainDumpFlash && createPortal(
                <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-[60] pointer-events-none">
                  <div
                    className="flex items-center gap-2 px-4 py-2.5 bg-surface-raised rounded-lg shadow-modal"
                    style={{
                      animation: 'toastFlash 1860ms ease-in-out forwards',
                    }}
                  >
                    <svg
                      className="text-success w-5 h-5"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2}
                      aria-hidden="true"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                    <p className="text-ink-body font-medium text-sm">{t('captured')}</p>
                  </div>
                  <style>{`
                    @keyframes toastFlash {
                      0% { opacity: 0; transform: translateY(8px); }
                      19% { opacity: 1; transform: translateY(0); }
                      73% { opacity: 1; transform: translateY(0); }
                      100% { opacity: 0; transform: translateY(8px); }
                    }
                  `}</style>
                </div>,
                document.body
              )}

              <div className="mt-4 space-y-2">
                {/* Row 1: Cancel + hint + Capture */}
                <div className="flex items-center">
                  <Dialog.Close asChild>
                    <button className="px-4 py-2 text-sm text-ink-secondary hover:text-ink-heading focus:outline-none focus:ring-2 focus:ring-edge-focus rounded-md transition-colors duration-100">
                      {t('common:cancel')}
                    </button>
                  </Dialog.Close>
                  <span className="flex-1 text-xs text-ink-muted text-right mr-3">
                    {t('enterToCapture')}
                  </span>
                  <button
                    disabled={isBlank || mutation.isPending || brainDumpFlash || brainDumpPending}
                    onClick={() => mutation.mutate()}
                    className="px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors duration-100"
                  >
                    {mutation.isPending ? t('common:saving') : t('capture')}
                  </button>
                </div>
                {/* Row 2: hint + Keep capturing */}
                <div className="flex items-center justify-end">
                  <span className="text-xs text-ink-muted mr-3">
                    {t('ctrlEnterToKeepCapturing')}
                  </span>
                  <button
                    disabled={isBlank || mutation.isPending || brainDumpFlash || brainDumpPending}
                    onClick={handleBrainDumpSubmit}
                    className="px-4 py-2 text-sm font-medium text-primary-500 border border-primary-300 rounded-md hover:bg-primary-50 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors duration-100"
                  >
                    {t('keepCapturing')}
                  </button>
                </div>
              </div>
            </div>
          )}
        </Dialog.Content>
      </Dialog.Portal>

      {flyAwayRects && (
        <FlyAwayCard
          startRect={flyAwayRects.start}
          endRect={flyAwayRects.end}
          onComplete={() => setFlyAwayRects(null)}
        />
      )}
    </Dialog.Root>
  )
}
