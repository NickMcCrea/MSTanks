import java.net.InetSocketAddress
import akka.actor.ActorSystem
import scala.concurrent.duration._


object Main {
  
  def parseArgs(args: Array[String]):Map[String,String] = {
    args.headOption match {
      case Some(arg) =>{
        arg match {
          case arg if arg.startsWith("-ip") =>
            Map("ip" -> arg.split("=")(1)) ++ parseArgs(args.tail)
          case arg if arg.startsWith("-name") =>
            Map("name" -> arg.split("=")(1)) ++ parseArgs(args.tail)
          case arg if arg.startsWith("-port") =>
            Map("port" -> arg.split("=")(1)) ++ parseArgs(args.tail)
        }
      }
      case None => Map.empty
    }
  }
  
  def main(args: Array[String]): Unit = {
     val argM:Map[String,String] = parseArgs(args)
     println(s"Arguments ${argM}")
     
     
     val system = ActorSystem("something")
     val bot = system.actorOf(Bot.props(argM.getOrElse("name", "ScalaBot")), "Bot")
     val client = system.actorOf(TcpClient.props( new InetSocketAddress(argM.getOrElse("ip","localhost"), argM.getOrElse("port","8052").toInt), bot), "client")
     
     import system.dispatcher
     system.scheduler.schedule(100 milliseconds,1000 milliseconds,bot,"Do")
   }
  
}




