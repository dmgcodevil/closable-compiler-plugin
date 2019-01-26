package com.github.dmgcodevil.closable.example

import com.github.dmgcodevil.closable.annotations.closeOnShutdown
import org.slf4j.{Logger, LoggerFactory}

object App {

  val log: Logger = LoggerFactory.getLogger("App")

  def main(args: Array[String]): Unit = {

    @closeOnShutdown("close", true, "log")
    val p1: Publisher = new Publisher("p1", true)
    @closeOnShutdown("stop")
    val server = new Server("s1")

    server.start()
    p1.pubslish("test")

  }

}
