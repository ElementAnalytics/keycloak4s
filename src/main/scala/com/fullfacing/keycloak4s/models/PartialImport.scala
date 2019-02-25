package models

import models.enums.{Policy => policyEnum}

case class PartialImport(
                          clients: Option[List[Client]],
                          groups: Option[List[Group]],
                          identityProviders: Option[List[IdentityProvider]],
                          ifResourceExists: Option[String],
                          policy: Option[policyEnum],
                          roles: Option[Role],
                          users: Option[List[User]],
                        )