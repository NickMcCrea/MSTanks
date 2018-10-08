

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.ByteString
import TankMessage._

case object Ack extends Event

object TcpClient {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[TcpClient], remote, replies)
}

class TcpClient(remote: InetSocketAddress, listener: ActorRef) extends Actor with ActorLogging {

  import context.system

  IO(Tcp) ! Connect(remote)




  override def receive: Receive = {
    case CommandFailed(_: Connect) =>
      context stop self


    case c@Connected(remote, local) ⇒
      log.info("conected to {} ", remote.getAddress)
      val connection = sender()


      connection ! Register(self)
      listener ! "Start"
      log.info("Register")

      context become {
        case data: ByteString ⇒
          log.info("sending")
          connection ! Write(data)
        case CommandFailed(w: Write) ⇒
        // O/S buffer was full

        case Received(data) ⇒
          listener ! toTankMessage(data)
          log.info("received")
        case "close" ⇒
          connection ! Close
        case _: ConnectionClosed ⇒
          log.info("stoping")
          listener ! "Stop"

          context stop self
      }


  }
}
