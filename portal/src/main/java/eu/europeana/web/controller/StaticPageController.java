/*
 * Copyright 2007 EDL FOUNDATION
 *
 *  Licensed under the EUPL, Version 1.0 or as soon they
 *  will be approved by the European Commission - subsequent
 *  versions of the EUPL (the "Licence");
 *  you may not use this work except in compliance with the
 *  Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  http://ec.europa.eu/idabc/eupl
 *
 *  Unless required by applicable law or agreed to in
 *  writing, software distributed under the Licence is
 *  distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied.
 *  See the Licence for the specific language governing
 *  permissions and limitations under the Licence.
 */

package eu.europeana.web.controller;

import eu.europeana.core.database.domain.StaticPageType;
import eu.europeana.core.util.web.ClickStreamLogger;
import eu.europeana.core.util.web.ControllerUtil;
import eu.europeana.core.util.web.StaticPageCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * Genereric controller for static pages. Badly deprecated, see StaticPageCOntroller in portal-full
 *
 * @author Eric van der Meulen <eric.meulen@gmail.com>
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 */

@Controller
@Deprecated
public class StaticPageController {

    @Autowired
    private StaticPageCache staticPageCache;

    @Autowired
    private ClickStreamLogger clickStreamLogger;

    /**
     * All of the pages are served up from here
     *
     * @param pageName name of the page
     * @param request  where we find locale
     * @throws Exception something went wrong
     * @return ModelAndView
     */

    @RequestMapping("/{pageName}.html")
    public ModelAndView fetchStaticPage(
            @PathVariable("pageName") String pageName,
            HttpServletRequest request
    ) {
        String pageValue = staticPageCache.getPage(null, pageName, ControllerUtil.getLocale(request));
        ModelAndView pageModel = ControllerUtil.createModelAndViewPage("static-page");
        if (pageValue != null) {
            pageModel.addObject("pageValue", pageValue);
        }
        clickStreamLogger.logCustomUserAction(request, ClickStreamLogger.UserAction.STATICPAGE, "view="+ pageName);
        return pageModel;
    }

    /*
    * freemarker template not loadable from database
    */

//    @RequestMapping("/advancedsearch.html")
//    public ModelAndView advancedSearchHandler(HttpServletRequest request) {
//        StaticPageType pageType = StaticPageType.ADVANCED_SEARCH;
//        clickStreamLogger.logStaticPageView(request, pageType);
//        return ControllerUtil.createModelAndViewPage(pageType.getViewName());
//    }
//

    /*
     * freemarker Template not loadable from database
     */

    @RequestMapping("/error.html")
    public ModelAndView errorPageHandler(HttpServletRequest request) {
        StaticPageType pageType = StaticPageType.ERROR;
        clickStreamLogger.logStaticPageView(request, pageType);
        return ControllerUtil.createModelAndViewPage(pageType.getViewName());
    }

}