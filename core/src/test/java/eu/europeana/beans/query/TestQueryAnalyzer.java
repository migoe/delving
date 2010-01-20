/*
 * Copyright 2007 EDL FOUNDATION
 *
 * Licensed under the EUPL, Version 1.0 or - as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.europeana.beans.query;

import eu.europeana.beans.AllFieldBean;
import eu.europeana.beans.BriefBean;
import eu.europeana.beans.FullBean;
import eu.europeana.beans.IdBean;
import eu.europeana.beans.annotation.AnnotationProcessorImpl;
import eu.europeana.query.EuropeanaQueryException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Make sure the query analyzer understands the different query types
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class TestQueryAnalyzer {

    private QueryAnalyzer qa;
    private AnnotationProcessorImpl annotationProcessor;

    @Before
    public void init() {
        qa = new QueryAnalyzer();
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(IdBean.class);
        list.add(BriefBean.class);
        list.add(FullBean.class);
        list.add(AllFieldBean.class);
        annotationProcessor = new AnnotationProcessorImpl();
        annotationProcessor.setClasses(list);
        qa.setAnnotationProcessor(annotationProcessor);
    }

    @Test
    public void variety() throws Exception {
        simple("very simple query");
        advanced("advanced and Query");
        advanced("text:fieldedQuery");
        advanced("+\"phrase query\"");
        moreLike("europeana_uri:\"http://www.europeana.eu/bla/bla\"");
    }

    @Test
    public void sanitize() {
        sanitize("hello { and { goodbye }", "hello AND goodbye");
    }

    @Test(expected = EuropeanaQueryException.class)
    public void testIllegalSearchField() throws Exception {
        advanced("dc_title:dimitry");
        advanced("gumby:goes_bad");
    }

    private void sanitize(String from, String to) {
        assertEquals("Not sanitized properly", to, qa.sanitize(from));
    }

    private void simple(String query) throws EuropeanaQueryException {
        assertEquals("Not a simple query ["+query+"]", QueryType.SIMPLE_QUERY, qa.findSolrQueryType(query));
    }

    private void advanced(String query) throws EuropeanaQueryException {
        assertEquals("Not an advanced query ["+query+"]", QueryType.ADVANCED_QUERY, qa.findSolrQueryType(query));
    }

    private void moreLike(String query) throws EuropeanaQueryException {
        assertEquals("Not a more-like query ["+query+"]", QueryType.MORE_LIKE_THIS_QUERY, qa.findSolrQueryType(query));
    }


}
