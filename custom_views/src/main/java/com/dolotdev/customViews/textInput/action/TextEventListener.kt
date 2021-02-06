package com.dolotdev.customViews.textInput.action

interface TextEventListener {
	fun onSuccess(message: String?)
	fun onError(message: String?)
}