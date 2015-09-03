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

import javax.inject.Inject

import play.api._
import play.api.mvc._
import play.api.i18n._

class Index @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

    val logger: Logger = Logger(this.getClass())

    def index = Action { implicit request => {
            logger.debug("accept languages are: " + request.acceptLanguages)

            Ok(views.html.index())
        }
    }

}
