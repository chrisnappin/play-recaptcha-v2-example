package controllers

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.i18n.MessagesApi
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

/**
  * Tests the <code>Index</code> class.
  */
@RunWith(classOf[JUnitRunner])
class IndexSpec extends PlaySpecification with Mockito {

  "Index controller" should {

    "Show the index page" in new WithApplication {
      val messagesApi = app.injector.instanceOf[MessagesApi]
      val controller = new Index(messagesApi)
      val request = FakeRequest(GET, "/")
      val page = controller.index.apply(request)

      status(page) must equalTo(OK)
      contentType(page) must beSome("text/html")
      contentAsString(page) must contain("Google Recaptcha v2 Integration")
    }
  }
}
