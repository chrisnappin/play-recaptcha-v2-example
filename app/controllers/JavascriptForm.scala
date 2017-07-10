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

import com.nappin.play.recaptcha.{NonceActionBuilder, RecaptchaVerifier, WidgetHelper}
import play.api.Logger
import play.api.data.Forms._
import play.api.data.Form
import play.api.i18n.{I18nSupport, Lang}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

import scala.concurrent.{ExecutionContext, Future}

// case class used to bind data from the form
case class JavascriptRegistration(username: String, email: Option[String], age: Int, agree: Boolean)

/**
  * Example form using Javascript and recaptcha.
  * @param formTemplate       The form template to use
  * @param nonceAction        Adds a nonce to the request, and CSP header to the response
  * @param verifier           The recaptcha verifier to use
  * @param widgetHelper       The widget helper to use
  * @param cc                 The controller components
  * @param executionContext   The execution context used to run futures
  */
class JavascriptForm @Inject()(formTemplate: views.html.javascriptForm, nonceAction: NonceActionBuilder,
  verifier: RecaptchaVerifier, widgetHelper: WidgetHelper, cc: ControllerComponents)(
  implicit executionContext: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  /** The logger to use. */
  private val logger = Logger(this.getClass)

  /** The form mapping and validations. */
  val userForm = Form(mapping(
    "username" -> nonEmptyText,
    "email" -> optional(email),
    "age" -> number,
    "agree" -> boolean
  )(JavascriptRegistration.apply)(JavascriptRegistration.unapply))

  /**
    * Show the Javascript form.
    * @return The form
    */
  def show = nonceAction { implicit request: Request[AnyContent] =>
    Ok(formTemplate(userForm))
  }

  /**
    * Load the data to pre-populate the form.
    * @return The form data
    */
  def load = Action.async { implicit request: Request[AnyContent] =>
    Future {
      logger.debug("loading data for javascript form...")

      // could go away and load this data from a database or sub-system perhaps...
      val exampleData = JavascriptRegistration("user1", Some("user1@abc.com"), 42, true)

      // write response as JSON
      implicit val format = Json.format[JavascriptRegistration]
      Ok(format.writes(exampleData))
    }
  }

  /**
    * Process the form, validate the data and check the captcha.
    * @return The success messages, or the error messages
    */
  def submitForm = Action.async { implicit request: Request[AnyContent] =>
    verifier.bindFromRequestAndVerify(userForm).map { form =>
      form.fold(

        // validation or captcha test failed
        errors => {
          logger.info("form validation or captcha test failed")

          // return the form validation errors
          UnprocessableEntity(widgetHelper.resolveRecaptchaErrors("captcha", errors).errorsAsJson)
        },

        // all validation and captcha passed
        success => {
          logger.info("Binding successful " + success)

          // TODO: process the validated form

          // return the result messages
          val lang: Lang = request.messages.lang
          Ok(Json.obj("title" -> messagesApi("result.title")(lang), "feedback" -> messagesApi("result.feedback")(lang)))
        }
      )
    }
  }
}
