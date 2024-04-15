package io.stempedia.pictoblox.firebase.login

class CheckUsernameResponse(val status: String, val email: String?, val isAvailable: Boolean, val error: String) {
}