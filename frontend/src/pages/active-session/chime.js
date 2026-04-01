export function playCompletionChime() {
  try {
    const ctx = new AudioContext()
    const t = ctx.currentTime
    const duration = 4.0

    // Warm fundamental tone ~220 Hz (A3)
    const osc1 = ctx.createOscillator()
    const gain1 = ctx.createGain()
    osc1.type = 'sine'
    osc1.frequency.setValueAtTime(220, t)
    gain1.gain.setValueAtTime(0, t)
    gain1.gain.linearRampToValueAtTime(0.3, t + 0.02)
    gain1.gain.exponentialRampToValueAtTime(0.001, t + duration)
    osc1.connect(gain1)
    gain1.connect(ctx.destination)

    // Upper partial ~550 Hz for warmth
    const osc2 = ctx.createOscillator()
    const gain2 = ctx.createGain()
    osc2.type = 'sine'
    osc2.frequency.setValueAtTime(550, t)
    gain2.gain.setValueAtTime(0, t)
    gain2.gain.linearRampToValueAtTime(0.15, t + 0.02)
    gain2.gain.exponentialRampToValueAtTime(0.001, t + duration * 0.6)
    osc2.connect(gain2)
    gain2.connect(ctx.destination)

    osc1.start(t)
    osc1.stop(t + duration)
    osc2.start(t)
    osc2.stop(t + duration * 0.6)

    osc1.onended = () => ctx.close()
  } catch {
    // AudioContext unavailable — silently ignore
  }
}
