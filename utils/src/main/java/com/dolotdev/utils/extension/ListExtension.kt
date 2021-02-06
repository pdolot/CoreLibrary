package com.dolotdev.utils.extension

fun <T> List<T>.linearSet(): List<T>{
	val list = arrayListOf<T>()
	list.add(this.first())
	for (i in 1 until this.size){
		if(list.last() != this[i]){
			list.add(this[i])
		}
	}
	return list
}