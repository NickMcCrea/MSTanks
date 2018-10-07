import java.net.InetSocketAddress
import akka.actor.ActorSystem
import scala.concurrent.duration._


object Main extends App {


  val system = ActorSystem("TankBot")

  val bot = system.actorOf(Bot.props, "Bot")

  val client = system.actorOf(TcpClient.props( new InetSocketAddress("localhost", 8052), bot), "client")

  import system.dispatcher
  system.scheduler.schedule(100 milliseconds,1000 milliseconds,bot,"Do")

}




