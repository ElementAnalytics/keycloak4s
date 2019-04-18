package com.fullfacing.keycloak4s

import java.util.UUID

import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.implicits._
import com.fullfacing.keycloak4s.client.serialization.JsonFormats._
import com.fullfacing.keycloak4s.client.{Keycloak, KeycloakClient, KeycloakConfig}
import com.fullfacing.keycloak4s.models.Client
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp.{MonadError, Request, Response, SttpBackend}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.json4s.jackson.Serialization

import scala.concurrent.Future
import scala.language.postfixOps

object Main extends App {

  implicit val sttpBackend: AkkaHttpBackendL = new AkkaHttpBackendL(AkkaHttpBackend())

  val config = KeycloakConfig("http", "localhost", 8080, "Retry", KeycloakConfig.Auth("master", "admin-cli", "6808820a-b662-4480-b832-f2d024eb6e03"))

  implicit val client: KeycloakClient[Task, Source[ByteString, Any]] =
    new KeycloakClient[Task, Source[ByteString, Any]](config)

  val clients = Keycloak.Clients[Task, Source[ByteString, Any]]

//  clients.fetchById(UUID.fromString("4dd218bf-ab82-4d5a-8c16-bee1fccee587")).foreachL(s => println(Serialization.writePretty(s))).onErrorHandle(_.printStackTrace()).runToFuture
    clients.update(UUID.fromString("89a62f04-75da-4ca5-bc0b-fcc4b5ac004c"), Client.Update(clientId = "CreateTest3", enabled = Some(false)))
      .foreachL(s => println(Serialization.writePretty(s))).onErrorHandle(_.printStackTrace()).runToFuture

  Console.readBoolean()
}

class AkkaHttpBackendL(delegate: SttpBackend[Future, Source[ByteString, Any]]) extends SttpBackend[Task, Source[ByteString, Any]] {
  override def send[T](request: Request[T, Source[ByteString, Any]]): Task[Response[T]] =
    Task.fromFuture(delegate.send(request))

  override def close(): Unit = delegate.close()

  override def responseMonad: MonadError[Task] = new MonadError[Task] {
    override def unit[T](t: T): Task[T] =
      Task.now(t)

    override def map[T, T2](fa: Task[T])(f: T => T2): Task[T2] =
      fa.map(f)

    override def flatMap[T, T2](fa: Task[T])(f: T => Task[T2]): Task[T2] =
      fa.flatMap(f)

    override def error[T](t: Throwable): Task[T] =
      Task.raiseError(t)

    override protected def handleWrappedError[T](rt: Task[T])(h: PartialFunction[Throwable, Task[T]]): Task[T] =
      rt.recoverWith(h)
  }
}