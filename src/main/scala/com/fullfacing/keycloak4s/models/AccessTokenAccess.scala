package models

case class AccessTokenAccess(
                              roles: Option[List[String]],
                              verify_caller: Option[Boolean]
                            )