import { createContext, useState, useEffect, useCallback } from 'react'
import { login as apiLogin, refreshToken } from '@/api/auth'
import { setAuthToken } from '@/api/client'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null)
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  const storeToken = useCallback((accessToken) => {
    setToken(accessToken)
    setAuthToken(accessToken)
  }, [])

  // On mount: attempt to restore session from the HttpOnly refresh cookie
  useEffect(() => {
    refreshToken()
      .then((data) => {
        storeToken(data.accessToken)
        if (data.user) setUser(data.user)
      })
      .catch(() => {
        // No valid session — that's fine, user will see login page
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [storeToken])

  const login = useCallback(async (email, password) => {
    const data = await apiLogin(email, password)
    storeToken(data.accessToken)
    if (data.user) setUser(data.user)
    // Store email so HomePage can show it even if the API doesn't return user info
    setUser((prev) => prev ?? { email })
    return data
  }, [storeToken])

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
    setAuthToken(null)
  }, [])

  return (
    <AuthContext.Provider value={{ token, user, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  )
}
