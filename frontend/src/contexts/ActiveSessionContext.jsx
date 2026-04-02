import { createContext, useContext, useState, useCallback } from 'react'

const ActiveSessionContext = createContext(null)

export function ActiveSessionProvider({ children }) {
  const [session, setSession] = useState(null)

  const startSession = useCallback((blockId, taskName, endTime) => {
    setSession({ blockId, taskName, endTime })
  }, [])

  const clearSession = useCallback(() => {
    setSession(null)
  }, [])

  return (
    <ActiveSessionContext.Provider value={{ session, startSession, clearSession }}>
      {children}
    </ActiveSessionContext.Provider>
  )
}

export function useActiveSession() {
  const ctx = useContext(ActiveSessionContext)
  if (!ctx) throw new Error('useActiveSession must be used within ActiveSessionProvider')
  return ctx
}
