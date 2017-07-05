/*
 * Copyright 2017 Chris Nappin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers

import javax.inject.Inject

import com.nappin.play.recaptcha.RecaptchaVerifier
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

import scala.concurrent.ExecutionContext

// case class used to bind data from the form
case class UserRegistration(username: String, email: Option[String], agree: Boolean)

/**
  * Example form with a recaptcha field.
  *
  * @param formTemplate       The form template to use
  * @param verifier           The recaptcha verifier to use
  * @param cc                 The controller components to use
  * @param executionContext   The execution context used to run futures
  */
class ExampleForm @Inject()(formTemplate: views.html.form, verifier: RecaptchaVerifier, cc: ControllerComponents)(
    implicit executionContext: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  /** The logger to use. */
  private val logger = Logger(this.getClass)

  /** Form data mapping and validation. */
  val userForm = Form(mapping(
    "username" -> nonEmptyText,
    "email" -> optional(email),
    "agree" -> boolean
  )(UserRegistration.apply)(UserRegistration.unapply))

  /**
    * Shows the form.
    *
    * @return The form
    */
  def show = Action { implicit request: Request[AnyContent] =>
    Ok(formTemplate(userForm))
  }

  /**
    * Handles a form submission.
    * @return The success redirect, or the form with error messages
    */
  def submitForm = Action.async { implicit request: Request[AnyContent] =>
    verifier.bindFromRequestAndVerify(userForm).map { form =>
      form.fold(
        // validation or captcha test failed
        errors => {
          // re-renders the form, with validation error messages etc
          logger.info("form validation or captcha test failed")
          BadRequest(formTemplate(errors))
        },

        // validation and captcha test succeeded
        success => {
          logger.info("User is " + success)

          // TODO: process the validated form

          // only store simple message in flash
          val saveMessage = "User " + success.username + " has been registered"

          // use POST-Redirect-GET to avoid repeated form submissions on browser refresh
          Redirect(routes.ExampleForm.result()).flashing("save.message" -> saveMessage)

          /*
           * This process uses GET redirect to a page with a message passed via flash scope.
           * Alternatives could be passing an id as request parameter to a page that then loads the data
           * again, or saving the model object in cache then reading from that...
           */
        }
      )
    }
  }

  /**
    * Show the result message.
    * @return The success result page
    */
  def result = Action { implicit request: Request[AnyContent]  =>
    // success message after POST-Redirect-GET
    // use flash scope to pass any once-off messages
    Ok(views.html.result())
  }

}
