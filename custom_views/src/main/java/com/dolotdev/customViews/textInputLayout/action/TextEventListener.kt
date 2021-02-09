package com.dolotdev.customViews.textInputLayout.action

interface TextEventListener {
	fun onSuccess(message: String?)
	fun onError(message: String?)
}