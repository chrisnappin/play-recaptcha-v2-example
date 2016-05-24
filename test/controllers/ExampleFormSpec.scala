/*
 * Copyright 2016 Chris Nappin
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

import com.nappin.play.recaptcha.{RecaptchaVerifier, WidgetHelper}

import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import org.junit.runner.RunWith

import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}
import com.nappin.play.recaptcha.RecaptchaSettings
import play.api.Application

/**
 * Unit test for the <code>ExampleForm</code> controller, using a mocked out Verifier.
 */
@RunWith(classOf[JUnitRunner])
class ExampleFormSpec extends PlaySpecification with Mockito {

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    "The example form controller" should {

        "return the example form" in new WithApplication with WithWidgetHelper {
            val controller = getMockController(app, VERIFIER_ACTION_NONE)
            val request = FakeRequest(GET, "/form")
            val page = controller.show().apply(request)

            status(page) must equalTo(OK)
            contentType(page) must beSome.which(_ == "text/html")
            contentAsString(page) must contain ("Example Form")
        }

        "reject an empty form submission" in new WithApplication {
            val controller = getMockController(app, VERIFIER_ACTION_EMPTY_FORM)
            val request = FakeRequest(POST, "/form").withFormUrlEncodedBody()

            await(controller.submitForm().apply(request)) must throwAn[IllegalStateException]
        }

        "reject missing mandatory fields" in new WithApplication with WithWidgetHelper {
            val controller = getMockController(app, VERIFIER_ACTION_USERNAME_MISSING)
            val request = FakeRequest(POST, "/form").withFormUrlEncodedBody(
                    "recaptcha_response_field" -> "r")
            val page = controller.submitForm().apply(request)

            status(page) must equalTo(BAD_REQUEST)
            contentType(page) must beSome.which(_ == "text/html")
            contentAsString(page) must contain ("Username is required")
        }

        "reject recaptcha failure" in new WithApplication with WithWidgetHelper {
            val controller = getMockController(app, VERIFIER_ACTION_RECAPTCHA_FAILURE)
            val request = FakeRequest(POST, "/form").withFormUrlEncodedBody(
                    "recaptcha_response_field" -> "r")
            val page = controller.submitForm().apply(request)

            status(page) must equalTo(BAD_REQUEST)
            contentType(page) must beSome.which(_ == "text/html")
            contentAsString(page) must contain ("Incorrect, please try again")
        }

        "handle recaptcha success" in new WithApplication with WithWidgetHelper {
            val controller = getMockController(app, VERIFIER_ACTION_RECAPTCHA_SUCCESS)
            val request = FakeRequest(POST, "/form").withFormUrlEncodedBody(
                    "recaptcha_response_field" -> "r",
                    "username" -> "a")

            val page = controller.submitForm().apply(request)
            status(page) must equalTo(SEE_OTHER)
        }

        "return the results page" in new WithApplication with WithWidgetHelper {
            val controller = getMockController(app, VERIFIER_ACTION_NONE)
            val request = FakeRequest(GET, "/result")
            val page = controller.result().apply(request)

            status(page) must equalTo(OK)
            contentType(page) must beSome.which(_ == "text/html")
            contentAsString(page) must contain ("User Registered")
        }
    }

    val VERIFIER_ACTION_NONE = 0
    val VERIFIER_ACTION_EMPTY_FORM = 1
    val VERIFIER_ACTION_USERNAME_MISSING = 2
    val VERIFIER_ACTION_RECAPTCHA_FAILURE = 3
    val VERIFIER_ACTION_RECAPTCHA_SUCCESS = 4

    def getMockController(app: Application, verifierAction: Int): ExampleForm = {
        implicit val widgetHelper = mock[WidgetHelper]
        val messagesApi = app.injector.instanceOf[MessagesApi]
        val verifier = mock[RecaptchaVerifier]
        val controller = new ExampleForm(messagesApi, verifier)

        verifierAction match {
            case VERIFIER_ACTION_NONE =>
                // does nothing
                ;

            case VERIFIER_ACTION_EMPTY_FORM =>
                // simulates empty form submission
                verifier.bindFromRequestAndVerify(any[play.api.data.Form[UserRegistration]])(
                    any[Request[AnyContent]],any[ExecutionContext]) throws new IllegalStateException("Oops")

            case VERIFIER_ACTION_USERNAME_MISSING =>
                // simulates username field (mandatory field) missing
                verifier.bindFromRequestAndVerify(any[play.api.data.Form[UserRegistration]])(
                    any[Request[AnyContent]],any[ExecutionContext]) returns
                        Future { controller.userForm.withError("username", "Username is required") }

            case VERIFIER_ACTION_RECAPTCHA_FAILURE =>
                // simulates recaptcha response incorrect
                verifier.bindFromRequestAndVerify(any[play.api.data.Form[UserRegistration]])(
                    any[Request[AnyContent]],any[ExecutionContext]) returns
                        Future { controller.userForm.withError(
                            RecaptchaVerifier.formErrorKey, "incorrect-captcha-sol") }

            case VERIFIER_ACTION_RECAPTCHA_SUCCESS =>
                val request = FakeRequest(POST, "/form").withFormUrlEncodedBody(
                    "recaptcha_response_field" -> "r",
                    "username" -> "a")

                verifier.bindFromRequestAndVerify(any[play.api.data.Form[UserRegistration]])(
                    any[Request[AnyContent]],any[ExecutionContext]) returns
                        Future { controller.userForm.bindFromRequest()(request) }
        }
        controller
    }

    trait WithWidgetHelper extends Scope {
        def app: play.api.Application
        lazy val settings = new RecaptchaSettings(app.configuration)
        lazy val widgetHelper = new WidgetHelper(settings)
    }
}
