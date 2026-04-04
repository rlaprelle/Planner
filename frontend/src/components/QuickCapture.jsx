import { useState, useEffect, useRef } from 'react'
import * as Dialog from '@radix-ui/react-dialog'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createDeferredItem } from '@/api/deferred'

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
  const [open, setOpen] = useState(false)
  const [text, setText] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const [error, setError] = useState(null)

  const queryClient = useQueryClient()
  const timeoutRef = useRef(null)

  const mutation = useMutation({
    mutationFn: () => createDeferredItem(text.trim()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['deferred'] })
      playChime()
      setConfirmed(true)
      timeoutRef.current = setTimeout(() => {
        setOpen(false)
        setConfirmed(false)
        setText('')
        setError(null)
      }, 1000)
    },
    onError: () => {
      setError("Couldn't save — try again.")
    },
  })

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current)
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
    } else {
      setOpen(true)
    }
  }

  function handleTextareaKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
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
          + Quick capture
        </button>
      </Dialog.Trigger>

      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/30 z-40" />
        <Dialog.Content
          className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-md bg-surface-raised rounded-xl shadow-modal p-6 focus:outline-none"
          aria-label="Quick capture"
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
              <p className="text-ink-body font-medium">Captured.</p>
            </div>
          ) : (
            <>
              <Dialog.Title className="text-base font-semibold text-ink-heading mb-3">
                Quick capture
              </Dialog.Title>
              <Dialog.Description className="sr-only">
                Type a quick thought to save it to your inbox.
              </Dialog.Description>

              <textarea
                autoFocus
                rows={3}
                className="w-full rounded-md border border-edge px-3 py-2 text-sm text-ink-heading placeholder-ink-muted resize-none focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus"
                placeholder="What's on your mind?"
                value={text}
                onChange={e => { setText(e.target.value); setError(null) }}
                onKeyDown={handleTextareaKeyDown}
              />

              {error && (
                <p className="mt-1 text-xs text-error" role="alert">{error}</p>
              )}

              <div className="mt-4 flex justify-end gap-2">
                <Dialog.Close asChild>
                  <button className="px-4 py-2 text-sm text-ink-secondary hover:text-ink-heading focus:outline-none focus:ring-2 focus:ring-edge-focus rounded-md transition-colors duration-100">
                    Cancel
                  </button>
                </Dialog.Close>
                <button
                  disabled={isBlank || mutation.isPending}
                  onClick={() => mutation.mutate()}
                  className="px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors duration-100"
                >
                  {mutation.isPending ? 'Saving…' : 'Capture'}
                </button>
              </div>
            </>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
