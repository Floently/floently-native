package com.floently.shared.auth

interface FloentlySessionStore {
    val session: FloentlyAuthSession?

    fun save(session: FloentlyAuthSession)

    fun clear()
}
