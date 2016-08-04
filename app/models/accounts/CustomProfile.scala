package models.accounts

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.SocialProfile


case class CustomProfile(loginInfo: LoginInfo) extends SocialProfile
