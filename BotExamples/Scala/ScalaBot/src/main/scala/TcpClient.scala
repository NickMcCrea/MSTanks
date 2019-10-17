import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.ByteString
import TankMessage._

import scala.collection.mutable.ArrayBuffer

case object Ack extends Event

object TcpClient {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[TcpClient], remote, replies)
}

class TcpClient(remote: InetSocketAddress, listener: ActorRef) extends Actor with ActorLogging {

  var msgs: ByteString = ByteString.empty
  def BtoUInt(b: Byte) = b & 0xFF

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
          msgs = msgs ++ data
          while ( msgs.nonEmpty && BtoUInt(msgs(1))+2 <= msgs.length){
            listener ! toTankMessage(msgs.take(BtoUInt(msgs(1))+2))
            msgs = msgs.drop(BtoUInt(msgs(1))+2)
          }
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
