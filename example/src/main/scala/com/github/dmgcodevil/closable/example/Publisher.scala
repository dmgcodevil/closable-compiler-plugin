package com.github.dmgcodevil.closable.example

class Publisher(val name: String, throwOnClose: Boolean = false) {

  def pubslish(msg: String): Unit = {
    println(s"Publisher[$name] publish: " + msg)
  }

  def close(): Unit = {
    if (throwOnClose) {
      throw new RuntimeException(s"Publisher[$name] thrown an error during closing")
    }
    println(s"Publisher[$name] closing")
  }

}