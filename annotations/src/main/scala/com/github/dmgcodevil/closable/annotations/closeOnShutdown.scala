package com.github.dmgcodevil.closable.annotations

import scala.annotation.StaticAnnotation

class closeOnShutdown(val value: String = "close", useLogger: Boolean = false, loggerName: String = "logger") extends StaticAnnotation
