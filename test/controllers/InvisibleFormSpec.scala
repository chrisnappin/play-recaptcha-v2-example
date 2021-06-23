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

import com.nappin.play.recaptcha.RecaptchaSettings._
import com.nappin.play.recaptcha.{NonceActionBuilder, RecaptchaVerifier}
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, ControllerComponents, MessagesActionBuilder, Request}
import play.api.data.FormBinding
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import play.api.test.CSRFTokenHelper._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Unit test for the <code>InvisibleForm</code> controller, using a mocked out Verifier.
  */
@RunWith(classOf[JUnitRunner])
class InvisibleFormSpec extends PlaySpecification with Mockito {

  private implicit val context = ExecutionContext.Implicits.global

  private val configuration: Map[String, String] =  Map(
    PrivateKeyConfigProp -> "private-key",
    PublicKeyConfigProp -> "public-key",
    RequestTimeoutConfigProp -> "5 seconds")

  abstract class WithWidgetHelper(configProps: Map[String, AnyRef]) extends WithApplication(
      GuiceApplicationBuilder().configure(configProps).build()) with Scope

  "The invisible form controller" should {

    "return the example form" in new WithWidgetHelper(configuration) {
      val controller = getController(app, VERIFIER_ACTION_NONE)
      val request = FakeRequest(GET, "/invisibleForm").withCSRFToken
      val page = controller.show().apply(request)

      status(page) must equalTo(OK)
      contentType(page) must beSome("text/html")
      contentAsString(page) must contain("Example Form")
    }

    "reject an empty form submission" in new WithWidgetHelper(configuration) {
      val controller = getController(app, VERIFIER_ACTION_EMPTY_FORM)
      val request = FakeRequest(POST, "/invisibleForm")
        .withFormUrlEncodedBody()
        .withCSRFToken

      await(controller.submitForm().apply(request)) must throwAn[IllegalStateException]
    }

    "reject missing mandatory fields" in new WithWidgetHelper(configuration) {
      val controller = getController(app, VERIFIER_ACTION_USERNAME_MISSING)
      val request = FakeRequest(POST, "/invisibleForm")
        .withFormUrlEncodedBody("recaptcha_response_field" -> "r")
        .withCSRFToken
      val page = controller.submitForm().apply(request)

      status(page) must equalTo(BAD_REQUEST)
      contentType(page) must beSome.which(_ == "text/html")
      contentAsString(page) must contain("Username is required")
    }

    "reject recaptcha failure" in new WithWidgetHelper(configuration) {
      val controller = getController(app, VERIFIER_ACTION_RECAPTCHA_FAILURE)
      val request = FakeRequest(POST, "/invisibleForm")
        .withFormUrlEncodedBody("recaptcha_response_field" -> "r")
        .withCSRFToken
      val page = controller.submitForm().apply(request)

      status(page) must equalTo(BAD_REQUEST)
      contentType(page) must beSome.which(_ == "text/html")
    }

    "handle recaptcha success" in new WithWidgetHelper(configuration) {
      val controller = getController(app, VERIFIER_ACTION_RECAPTCHA_SUCCESS)
      val request = FakeRequest(POST, "/invisibleForm")
        .withFormUrlEncodedBody(
          "recaptcha_response_field" -> "r",
          "username" -> "a")
        .withCSRFToken

      val page = controller.submitForm().apply(request)
      status(page) must equalTo(SEE_OTHER)
    }

    "return the results page" in new WithWidgetHelper(configuration) {
      val controller = getController(app, VERIFIER_ACTION_NONE)
      val request = FakeRequest(GET, "/invisibleResult").withCSRFToken
      val page = controller.result().apply(request)

      status(page) must equalTo(OK)
      contentType(page) must beSome.which(_ == "text/html")
      contentAsString(page) must contain("User Registered")
    }
  }

  val VERIFIER_ACTION_NONE = 0
  val VERIFIER_ACTION_EMPTY_FORM = 1
  val VERIFIER_ACTION_USERNAME_MISSING = 2
  val VERIFIER_ACTION_RECAPTCHA_FAILURE = 3
  val VERIFIER_ACTION_RECAPTCHA_SUCCESS = 4

  /**
    * Get a controller, with mock dependencies populated and primed with the specified behaviour.
    * @param app              The current play app
    * @param verifierAction   The verifier behaviour to prime
    * @return The controller
    */
  def getController(app: Application, verifierAction: Int): InvisibleForm = {
    val messagesActionBuilder = app.injector.instanceOf[MessagesActionBuilder]
    val formTemplate = app.injector.instanceOf[views.html.invisibleForm]
    val nonceAction = app.injector.instanceOf[NonceActionBuilder]
    val verifier = mock[RecaptchaVerifier]
    val cc = app.injector.instanceOf[ControllerComponents]
    val controller = new InvisibleForm(messagesActionBuilder, nonceAction, formTemplate, verifier, cc)

    verifierAction match {
      case VERIFIER_ACTION_NONE =>
        // does nothing
        ;

      case VERIFIER_ACTION_EMPTY_FORM =>
        // simulates empty form submission
        verifier.bindFromRequestAndVerify(any[play.api.data.Form[InvisibleUserRegistration]])(
          any[Request[AnyContent]], any[ExecutionContext]) throws new IllegalStateException("Oops")

      case VERIFIER_ACTION_USERNAME_MISSING =>
        // simulates username field (mandatory field) missing
        verifier.bindFromRequestAndVerify(any[play.api.data.Form[InvisibleUserRegistration]])(
          any[Request[AnyContent]], any[ExecutionContext]) returns
          Future {
            controller.userForm.withError("username", "Username is required")
          }

      case VERIFIER_ACTION_RECAPTCHA_FAILURE =>
        // simulates recaptcha response incorrect
        verifier.bindFromRequestAndVerify(any[play.api.data.Form[InvisibleUserRegistration]])(
          any[Request[AnyContent]], any[ExecutionContext]) returns
          Future {
            controller.userForm.withError(
              RecaptchaVerifier.formErrorKey, "incorrect-captcha-sol")
          }

      case VERIFIER_ACTION_RECAPTCHA_SUCCESS =>
        val request = FakeRequest(POST, "/invisibleForm").withFormUrlEncodedBody(
          "recaptcha_response_field" -> "r",
          "username" -> "a")

        val formBinding = FormBinding.Implicits.formBinding

        verifier.bindFromRequestAndVerify(any[play.api.data.Form[InvisibleUserRegistration]])(
          any[Request[AnyContent]], any[ExecutionContext]) returns
          Future {
            controller.userForm.bindFromRequest()(request, formBinding)
          }
    }
    controller
  }
}
