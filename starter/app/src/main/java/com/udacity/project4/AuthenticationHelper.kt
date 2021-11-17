package com.udacity.project4

import android.content.Context
import com.firebase.ui.auth.AuthUI

object AuthenticationHelper {
    fun signOut(context: Context) = AuthUI.getInstance().signOut(context)
    fun signIn() = AuthUI.getInstance().createSignInIntentBuilder()
}

enum class AuthenticationState {
    AUTHENTICATED, NOT_AUTHENTICATED
}