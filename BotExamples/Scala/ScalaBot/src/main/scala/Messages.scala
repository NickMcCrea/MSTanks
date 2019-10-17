import java.nio.charset.StandardCharsets
import java.time.LocalDate

import akka.util.ByteString

import scala.language.implicitConversions
import spray.json._

import scala.collection.mutable.ArrayBuffer

sealed trait Payload

final case class CreateTankPayload(Name: String) extends Payload

final case class AmountPayload(Amount: Int) extends Payload

final case class IDPayload(Id:Int) extends Payload

final case class UpdatePayload(Id: Int, Name: String, Type: String, X: Float, Y: Float, Heading: Float, TurretHeading: Float, Health: Int, Ammo: Int) extends Payload

final case class TimePayload(Time:Int) extends Payload

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val CreateTankPayloadFormat = jsonFormat1(CreateTankPayload)
  implicit val AmountPayloadFormat = jsonFormat1(AmountPayload)
  implicit val IDPayloadFormat = jsonFormat1(IDPayload)
  implicit val UpdatePayloadFormat = jsonFormat9(UpdatePayload)
  implicit val TimePayloadFormat = jsonFormat1(TimePayload)
}


case class TankMessage(messageType: String, oPayload: Option[Payload] = None)

object TankMessage {

  val MessageTypeToInt = Map(
    "test" -> 0,
    "createTank" -> 1,
    "despawnTank" -> 2,
    "fire" -> 3,
    "toggleForward" -> 4,
    "toggleReverse" -> 5,
    "toggleLeft" -> 6,
    "toggleRight" -> 7,
    "toggleTurretLeft" -> 8,
    "toggleTurretRight" -> 9,
    "turnTurretToHeading" -> 10,
    "turnToHeading" -> 11,
    "moveForwardDistance" -> 12,
    "moveBackwardsDistance" -> 13,
    "stopAll" -> 14,
    "stopTurn" -> 15,
    "stopMove" -> 16,
    "stopTurret" -> 17,
    "objectUpdate" -> 18,
    "healthPickup" -> 19,
    "ammoPickup" -> 20,
    "snitchPickup" -> 21,
    "destroyed" -> 22,
    "enteredGoal" -> 23,
    "kill" -> 24,
    "snitchAppeared" -> 25,
    "gameTimeUpdate" -> 26,
    "hitDetected" -> 27,
    "successfulHit" -> 28
  )

  val IntToMessageType = MessageTypeToInt.map(_.swap)

  def toByteString(msg: TankMessage): ByteString = {

    import MyJsonProtocol._

    msg match {
      case TankMessage(mType, None) =>
        ByteString(MessageTypeToInt(mType).toByte, 0.toByte)
      case TankMessage(mType, Some(payload)) =>
        val Apayload = payload match {
          case payload: CreateTankPayload => payload.toJson.compactPrint.getBytes()
          case payload: AmountPayload => payload.toJson.compactPrint.getBytes(StandardCharsets.US_ASCII)
        }
        ByteString(MessageTypeToInt(mType).toByte, Apayload.length.toShort.toByte) ++ Apayload
    }


  }

  def toTankMessage(data: ByteString): TankMessage = {
    
    /*
    TODO get rid off this mutabile coding style
      It works well but there must be a more idomatic way to do this
    */

    def BtoUInt(b: Byte) = b & 0xFF

    import MyJsonProtocol._
      data(0) match {
        case 18 =>
          val payload = data.drop(2).take(BtoUInt(data(1))).decodeString(StandardCharsets.US_ASCII).parseJson.convertTo[UpdatePayload]
          TankMessage(IntToMessageType(18), Some(payload))
        case 21 =>
          val payload = data.drop(2).take(BtoUInt(data(1))).decodeString(StandardCharsets.US_ASCII).parseJson.convertTo[IDPayload]
          TankMessage(IntToMessageType(21), Some(payload))
        case 26 =>
          val payload = data.drop(2).take(BtoUInt(data(1))).decodeString(StandardCharsets.US_ASCII).parseJson.convertTo[TimePayload]
          TankMessage(IntToMessageType(26), Some(payload))
        case mT=>
         TankMessage(IntToMessageType(mT.toInt))
      }
    }
}
