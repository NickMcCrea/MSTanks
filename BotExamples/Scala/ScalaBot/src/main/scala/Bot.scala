import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import TankMessage._
import akka.dispatch.forkjoin.ThreadLocalRandom

object Bot {
  def props(name:String) = Props(classOf[Bot], name)
}

class Bot(name:String) extends Actor with ActorLogging {
  val ran =  ThreadLocalRandom.current()

  override def receive: Receive = {

    case "Start" =>
      val TcpClient: ActorRef = sender()

      TcpClient ! toByteString(TankMessage("createTank", Some(CreateTankPayload(name))))

      context become {

        case "Do" =>
          //TODO this is where your bots logic would go
          TcpClient ! toByteString(TankMessage(IntToMessageType(14)))
          TcpClient ! toByteString(TankMessage(IntToMessageType(ran.nextInt(3,9))))
        case msg: TankMessage =>
          //TODO this is where your game object updates will go, you might decide its for logic as well
          log.info(msg.toString)
        case "Stop" =>
          log.info("stoping")
          context.system.terminate()
      }
  }
}


