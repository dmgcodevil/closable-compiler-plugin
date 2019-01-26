package com.github.dmgcodevil.closable.plugin

import scala.tools.nsc.plugins._
import scala.tools.nsc.transform._
import scala.tools.nsc.{Global, Phase}

class ClosableCompilerPlugin(override val global: Global)
  extends Plugin {
  override val name = "closable-compiler-plugin"
  override val description = "Closable compiler plugin"
  override val components =
    List(new CompilerPluginComponent(global))
}


class CompilerPluginComponent(val global: Global)
  extends PluginComponent with TypingTransformers {

  import CompilerPluginComponent._
  import global._

  override val phaseName = "compiler-plugin-phase"
  override val runsAfter = List("parser")


  override def newPhase(prev: Phase): StdPhase =
    new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {
        unit.body = new MyTypingTransformer(unit).transform(unit.body)
      }
    }

  class MyTypingTransformer(unit: CompilationUnit)
    extends TypingTransformer(unit) {
    override def transform(tree: Tree): global.Tree = tree match {
      case valDef@ValDef(mods: Modifiers, valName: TermName, _, valRhs) => {

        val closeOnShutdownAnn = mods.annotations.find {
          case Apply(Select(New(Ident(TypeName(`annotationName`))), _), _) => true
          case Apply(Select(New(Select(_, TypeName(`annotationName`))), _), _) => true
          case _ => false
        }

        closeOnShutdownAnn match {
          case Some(ann) => ann match {
            case Apply(_, annArgs) =>
              val defaultArgs = (TermName(defaultCloseMethod), Constant(false), TermName(defaultLoggerName))
              val (closeMethodName, useLogger, loggerName) = annArgs match {
                // todo: add support for names args
                case Literal(Constant(cmn: String)) :: Nil => defaultArgs.copy(_1 = TermName(cmn))
                case Literal(Constant(cmn: String)) :: Literal(ul@Constant(_: Boolean)) :: Nil => defaultArgs.copy(_1 = TermName(cmn), _2 = ul)
                case Literal(Constant(cmn: String)) :: Literal(ul@Constant(_: Boolean)) :: Literal(Constant(ln: String)) :: Nil => (TermName(cmn), ul, TermName(ln))
                case _ =>
                  println(s"unsupported args syntax: ${annArgs.mkString(";")}")
                  defaultArgs
              }

              val logOnErrorBlock =
                useLogger match {
                  case Constant(true) =>
                    q"$loggerName.error(ex.getMessage, ex)"

                  case _ => q"ex.printStackTrace()"
                }


              val newBlock =
                q"""
                         val useLogger = $useLogger
                         val tmp = $valRhs
                         sys.addShutdownHook {
                           try {
                             tmp.$closeMethodName
                           } catch {
                             case ex: Throwable =>
                               $logOnErrorBlock
                           }
                         }
                         tmp
               """

              valDef.copy(rhs = newBlock)

          }
          case _ => valDef
        }

      }
      case _ => super.transform(tree)
    }
  }


  def newTransformer(unit: CompilationUnit): MyTypingTransformer =
    new MyTypingTransformer(unit)
}

object CompilerPluginComponent {
  val annotationName = "closeOnShutdown"
  val defaultCloseMethod = "close"
  val defaultLoggerName = "logger"
}