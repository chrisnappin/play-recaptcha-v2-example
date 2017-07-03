package controllers

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.mvc.ControllerComponents
import play.api.test.{FakeRequest, Injecting, PlaySpecification, WithApplication}

/**
  * Tests the <code>Index</code> class.
  */
@RunWith(classOf[JUnitRunner])
class IndexSpec extends PlaySpecification with Mockito {

  "Index controller" should {

    "Show the index page" in new WithApplication() with Injecting {
      val cc = inject[ControllerComponents]
      val controller = new Index(cc)
      val request = FakeRequest(GET, "/")
      val page = controller.index.apply(request)

      status(page) must equalTo(OK)
      contentType(page) must beSome("text/html")
      contentAsString(page) must contain("Google Recaptcha v2 Integration")
    }
  }
}
