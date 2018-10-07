import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import TankMessage._
import akka.dispatch.forkjoin.ThreadLocalRandom





object Bot {
  def props = Props(classOf[Bot])
}


class Bot extends Actor with ActorLogging {
  val ran =  ThreadLocalRandom.current()

  override def receive: Receive = {



    case "Start" =>
      val TcpClient: ActorRef = sender()

      TcpClient ! toByteString(TankMessage("createTank", Some(CreateTankPayload("ScalaBot"))))

      context become {

        case "Do" =>

          //TODO this is where your bots logic would go
          TcpClient ! toByteString(TankMessage(IntToMessageType(14)))
          TcpClient ! toByteString(TankMessage(IntToMessageType(ran.nextInt(3,9))))
        case msg: TankMessage =>
          //TODO this is where your game object updates will go who might decide its for logic as well
          println(msg.messageType)
        case "Stop" =>
          log.info("stoping")
          context.system.terminate()
      }

  }
}


