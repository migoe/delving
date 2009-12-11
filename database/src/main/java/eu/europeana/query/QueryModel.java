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

package eu.europeana.query;

import java.util.List;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 */

public interface QueryModel {

    QueryExpression setQueryString(String queryString) throws EuropeanaQueryException;
    void setQueryExpression(QueryExpression queryExpression);
    void setQueryConstraints(Constraints constraints);
    void setResponseType(ResponseType responseType);
    void setStartRow(int startRow);
    int getStartRow();
    void setRows(int rows);
    int getRows();

    ResponseType getResponseType();
    Constraints getConstraints();
    String getQueryString();
    QueryExpression.QueryType getQueryType();
    RecordFieldChoice getRecordFieldChoice();
    
    ResultModel fetchResult() throws EuropeanaQueryException;

    public interface Constraints {

        public interface Entry {
            FacetType getFacetType();
            String getValue();
        }

        List<FacetType> getFacetTypes();
        List<String> getConstraint(FacetType type);
        List<? extends Entry> getEntries();
    }
}