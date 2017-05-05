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

import com.nappin.play.recaptcha.{RecaptchaVerifier, WidgetHelper}
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

// case class used to bind data from the form
case class JavascriptRegistration(username: String, email: Option[String], age: Int, agree: Boolean)

// Example using Javascript form.
class JavascriptForm @Inject()(val messagesApi: MessagesApi, val verifier: RecaptchaVerifier)(
        implicit widgetHelper: WidgetHelper) extends Controller with I18nSupport {

    val logger = Logger(this.getClass())

    val userForm = Form(mapping(
            "username" -> nonEmptyText,
            "email" -> optional(email),
            "age" -> number,
            "agree" -> boolean
	    )(JavascriptRegistration.apply)(JavascriptRegistration.unapply))

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    def show = Action { implicit request =>
        Ok(views.html.javascriptForm(userForm))
    }

    def load = Action.async { implicit request =>
        Future {
          logger.debug("loading data for javascript form...")

          // could go away and load this data from a database or sub-system perhaps...
          val jsRegistration = JavascriptRegistration("user1", Some("user1@abc.com"), 42, true)

          // write response as JSON
          implicit val format = Json.format[JavascriptRegistration]
          Ok(format.writes(jsRegistration))
        }
    }

    def submitForm = Action.async { implicit request =>

        // TODO: add recaptcha and verify it

        userForm.bindFromRequest.fold(
          errors => Future {
            // return the form validation errors
            logger.debug("Binding had errors " + errors.errors)
            UnprocessableEntity(errors.errorsAsJson)
          },
          success => Future {
            // TODO: replace form with result
            logger.debug("Binding successful " + success)

            // TODO: process the validated form

            // return the result messages
            Ok(Json.obj("title" -> messagesApi("result.title"),
                        "feedback" -> messagesApi("result.feedback")))
          }
        )

      /*
        verifier.bindFromRequestAndVerify(userForm).map { form =>
            form.fold(
                // validation or captcha test failed
	            errors => {
	                // re-renders the form, with validation error messages etc
	                logger.info("form validation or captcha test failed")
	            	BadRequest(views.html.javascriptForm(errors))
	            },

	            success => {
	                logger.info("User is " + success)

	                // only store simple message in flash
	                val saveMessage = "User " + success.username + " has been registered"
	                    //Messages("example.saveMessage", user)

	                // use POST-Redirect-GET to avoid repeated form submissions on browser refresh
	                Redirect(routes.ExampleForm.result)
	                	.flashing("save.message" -> saveMessage)

	                /*
	                 * This process uses GET redirect to a page with a message passed via flash scope.
	                 * Alternatives could be passing an id as request parameter to a page that then loads the data
	                 * again, or saving the model object in cache then reading from that...
	                 */
	            }
            )
        }
*/
    }

}
