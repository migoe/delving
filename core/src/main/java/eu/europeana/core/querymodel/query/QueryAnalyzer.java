/*
 * Copyright 2007 EDL FOUNDATION
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they
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

package eu.europeana.core.querymodel.query;

import eu.delving.metadata.MetadataModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The query is an advanced query when the query string contains - " AND ", " OR ", " NOT " (both uppercase) - a fielded
 * query (detected by the use of a : seperating field and query), e.g. title:"something - a word or phrase prefixed by +
 * or -
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 */

public class QueryAnalyzer {

    @Autowired
    private MetadataModel metadataModel;

    public QueryAnalyzer() {
    }

    public QueryAnalyzer(MetadataModel metadataModel) {
        this.metadataModel = metadataModel;
    }

    public QueryType findSolrQueryType(String query) throws EuropeanaQueryException {
        String[] terms = query.split("\\s+");
        for (String term : terms) {
            if (BOOLEAN_KEYWORDS.contains(term)) {
                return QueryType.ADVANCED_QUERY;
            }
            else if (term.contains("*:*")) {
                return QueryType.ADVANCED_QUERY;
            }
            else if (term.startsWith("+") || term.startsWith("-")) {
                return QueryType.ADVANCED_QUERY;
            }
            else if (term.indexOf(':') > 0) {
                int colon = term.indexOf(':');
                String field = term.substring(0, colon);
                if ("europeana_uri".equals(field)) {
                    return QueryType.MORE_LIKE_THIS_QUERY;
                }
                else {
                    if (metadataModel.getRecordDefinition().getFieldNameList().contains(field)) {
                        return QueryType.ADVANCED_QUERY;
                    }
                    else if ("tag".equalsIgnoreCase(field)) {
                        return QueryType.ADVANCED_QUERY;
                    }
                    else if ("text".equalsIgnoreCase(field)) {
                        return QueryType.ADVANCED_QUERY;
                    }
                    else {
                        throw new EuropeanaQueryException(QueryProblem.MALFORMED_QUERY.toString());
                    }
                }
            }
        }
        return QueryType.SIMPLE_QUERY;
    }

    public String sanitize(String query) {
        String[] terms = query.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String term : terms) {
            if (BOOLEAN_KEYWORDS.contains(term)) {
                term = term.toUpperCase();
            }
            boolean emptyTerm = true;
            for (int walk = 0; walk < term.length(); walk++) {
                char ch = term.charAt(walk);
                switch (ch) {
                    case '{':
                    case '}':
                        break;
                    default:
                        out.append(ch);
                        emptyTerm = false;
                }
            }
            if (!emptyTerm) {
                out.append(' ');
            }
        }
        return out.toString().trim();
    }

    private static final Set<String> BOOLEAN_KEYWORDS = new TreeSet<String>();

    private static void addBooleanKeyword(String keyword) {
        BOOLEAN_KEYWORDS.add(keyword);
        BOOLEAN_KEYWORDS.add(keyword.toLowerCase());
    }

    static {
        addBooleanKeyword("AND");
        addBooleanKeyword("OR");
        addBooleanKeyword("NOT");
    }


    /**
     * Create advanced query from params with facet[1-3], operator[1-3], query[1-3].
     * <p/>
     * This query is structured by the advanced search pane in the portal
     *
     * @param params request parameters
     * @return all parameters formatted as a single Lucene Query
     */

    public String createAdvancedQuery(Map<String, String[]> params) {
        StringBuilder queryString = new StringBuilder();
        for (int i = 1; i < 4; i++) {
            if (params.containsKey(MessageFormat.format("query{0}", i)) && params.containsKey(MessageFormat.format("facet{0}", i))) {
                String facetDefault = "text";
                String facet = params.get(MessageFormat.format("facet{0}", i))[0];
                String query = params.get(MessageFormat.format("query{0}", i))[0];
                String operator = null;
                if (i != 1) {
                    operator = params.get(MessageFormat.format("operator{0}", i))[0];
                }
                if (!query.isEmpty()) {
                    if (operator != null) {
                        queryString.append(" ").append(operator).append(" ");
                    }
                    if (!facet.isEmpty()) {
                        queryString.append(facet);
                    }
                    else {
                        queryString.append(facetDefault);
                    }
                    queryString.append(":").append(query);
                }
            }
        }
        return sanitize(queryString.toString());
    }

    public String createRefineSearchFilterQuery(Map<String, String[]> params) throws EuropeanaQueryException {
        String refineQuery = params.get("rq")[0];
        // check length
        String newQuery = "";
        if (refineQuery.trim().length() > 0) {
            if (refineQuery.contains(":")) {
                newQuery = MessageFormat.format("{0}", refineQuery);
            }
            else {
                newQuery = MessageFormat.format("text:\"{0}\"", sanitize(refineQuery));
            }
        }
        return sanitize(newQuery);
    }
}
