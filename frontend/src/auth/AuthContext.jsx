import { createContext, useState, useEffect, useCallback } from 'react'
import { login as apiLogin, logout as apiLogout, refreshToken } from '@/api/auth'
import { setAuthToken, setTokenRefreshedCallback, setAuthFailureCallback } from '@/api/client'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null)
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  const storeToken = useCallback((accessToken) => {
    setToken(accessToken)
    setAuthToken(accessToken)
  }, [])

  // Register callbacks so client.js can sync token and force logout
  useEffect(() => {
    setTokenRefreshedCallback(storeToken)
    setAuthFailureCallback(() => {
      setToken(null)
      setUser(null)
      setAuthToken(null)
    })
  }, [storeToken])

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
    setUser(data.user ?? { email })
  }, [storeToken])

  const logout = useCallback(async () => {
    try {
      await apiLogout()
    } catch {
      // Server call failed — proceed with client-side cleanup anyway
    }
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
