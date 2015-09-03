/*
 * Copyright 2014 Chris Nappin
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

import com.nappin.play.recaptcha.RecaptchaVerifier

import javax.inject.Inject

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.i18n._
import play.api.mvc._

// case class used to bind data from the form
case class UserRegistration(username: String, email: Option[String], agree: Boolean)

class ExampleForm @Inject() (val messagesApi: MessagesApi, val verifier: RecaptchaVerifier)
        extends Controller with I18nSupport {

    val logger = Logger(this.getClass())

    val userForm = Form(mapping(
            "username" -> nonEmptyText,
            "email" -> optional(email),
            "agree" -> boolean
	    )(UserRegistration.apply)(UserRegistration.unapply))

    def show = Action { implicit request =>
        Ok(views.html.form(userForm))
    }

    def submitForm = Action.async { implicit request =>
        implicit val context = scala.concurrent.ExecutionContext.Implicits.global

        verifier.bindFromRequestAndVerify(userForm).map { form =>
            form.fold(
                // validation or captcha test failed
	            errors => {
	                // re-renders the form, with validation error messages etc
	                logger.info("form validation or captcha test failed")
	            	BadRequest(views.html.form(errors))
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

    }

    def result = Action { implicit request =>
        // success message after POST-Redirect-GET
        // use flash scope to pass any once-off messages
        Ok(views.html.result())
    }

}
