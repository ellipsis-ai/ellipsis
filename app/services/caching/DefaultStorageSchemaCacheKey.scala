package services.caching

import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

case class DefaultStorageSchemaCacheKey(
                                         groupVersion: BehaviorGroupVersion,
                                         user: User
                                      )
