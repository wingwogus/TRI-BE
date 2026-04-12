package com.tribe.application.security

interface CurrentActor {
    fun requireUserId(): Long
}
