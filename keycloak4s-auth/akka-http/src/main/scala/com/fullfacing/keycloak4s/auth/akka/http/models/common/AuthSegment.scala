package com.fullfacing.keycloak4s.auth.akka.http.models.common

final case class AuthSegment(segment: String,
                             auth: List[MethodRoles])