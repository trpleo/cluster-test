package org.trp.cluster

import akka.http.scaladsl.marshalling.{ PredefinedToEntityMarshallers, ToEntityMarshaller }
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import akka.util.ByteString
import scalapb.json4s.JsonFormat
import scalapb.{ GeneratedMessage, GeneratedMessageCompanion, Message }

// todo: it would be much nicer, if the serializer would be controlled by request header...
// todo: create factory method based on header data
trait ProtobufSupport extends Config {

  implicit def protobufMarshaller[T <: GeneratedMessage]: ToEntityMarshaller[T] =
    jsonEnabled match {
      case true =>
        PredefinedToEntityMarshallers.ByteArrayMarshaller.compose[T] { message =>
          implicit val a = message.companion
          ByteString(JsonFormat.toJsonString(message)).toArray
        }
      case false =>
        PredefinedToEntityMarshallers.ByteArrayMarshaller.compose[T](r => r.toByteArray)
    }

  implicit def protobufUnmarshaller[T <: GeneratedMessage with Message[T]](implicit companion: GeneratedMessageCompanion[T]): FromEntityUnmarshaller[T] =
    jsonEnabled match {
      case true =>
        Unmarshaller.byteArrayUnmarshaller.map[T] { bytes =>
          JsonFormat.fromJsonString(ByteString(bytes).utf8String)
        }
      case false =>
        Unmarshaller.byteArrayUnmarshaller.map[T](bytes => companion.parseFrom(bytes))
    }
}
