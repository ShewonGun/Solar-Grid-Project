/*
 * File: useSession.js
 * Purpose: React hook that exposes the signed-in session to components and
 *          keeps it in step with the rest of the app. It re-reads storage when
 *          the axios interceptor reports that the Web API rejected the token,
 *          and when the user signs out in another browser tab.
 * Author:  <your name>
 * Created: 2026
 */
import { useCallback, useEffect, useState } from 'react'

import { SESSION_CHANGED_EVENT } from '../api/client'
import { clearSession, readSession } from './session'

/*
 * Returns { session, user, role, signOut } for the current user, or nulls when
 * nobody is signed in.
 */
export default function useSession() {
  const [session, setSession] = useState(() => readSession())

  useEffect(() => {
    // Re-reads storage whenever the session may have changed: a 401 from the
    // API in this tab, or a sign-in/sign-out in another tab.
    const refresh = () => setSession(readSession())

    window.addEventListener(SESSION_CHANGED_EVENT, refresh)
    window.addEventListener('storage', refresh)

    return () => {
      window.removeEventListener(SESSION_CHANGED_EVENT, refresh)
      window.removeEventListener('storage', refresh)
    }
  }, [])

  /* Clears the stored session and tells the rest of the app about it. */
  const signOut = useCallback(() => {
    clearSession()
    setSession(null)
    window.dispatchEvent(new Event(SESSION_CHANGED_EVENT))
  }, [])

  return {
    session,
    user: session?.user ?? null,
    role: session?.user?.role ?? null,
    signOut,
  }
}
