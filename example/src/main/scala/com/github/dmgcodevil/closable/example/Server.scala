package com.github.dmgcodevil.closable.example

class Server(val name: String) {

  def start(): Unit = {
    println(s"Server[$name] starting")
  }

  def stop(): Unit = {
    println(s"Server[$name] stopping")
  }
}
