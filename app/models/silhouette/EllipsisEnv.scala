package models.silhouette

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.accounts.user.User

trait EllipsisEnv extends Env {
  type I = User
  type A = CookieAuthenticator
}
