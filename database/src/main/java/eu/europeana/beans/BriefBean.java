package eu.europeana.beans;

import eu.europeana.query.BriefDoc;
import eu.europeana.query.BriefDocWindow;
import eu.europeana.query.DocType;
import org.apache.solr.client.solrj.beans.Field;

import static eu.europeana.beans.BeanUtil.returnStringOrElse;

/**
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 * @since Jan 7, 2010 9:16:46 AM
 */

@EuropeanaView(facets = false, rows = 10)
public class BriefBean extends RequiredBean implements BriefDoc {

    int index;

    @Field("TYPE")
    @Europeana(copyField = true, facet = true, facetPrefix = "type", briefDoc = true)
    @Solr(fieldType = "string")
    String[] docType;

    @Field("LANGUAGE")
    @Europeana(copyField = true, facet = true, facetPrefix = "lang", briefDoc = true)
    @Solr(fieldType = "string")
    String[] language;

    @Field("YEAR")
    @Europeana(copyField = true, facet = true, facetPrefix = "yr", briefDoc = true)
    @Solr(fieldType = "string")
    String[] year;

    @Field
    @Europeana(copyField = true, briefDoc = true)
    @Solr()
    String[] title;

    @Field
    @Solr()
    @Europeana(copyField = true, briefDoc = true)
    String creator;

    @Override
    public int getIndex() {
        return 0;  //Todo: implement this
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String getId() {
        return europeanaUri;
    }

    @Override
    public String getTitle() {
        return returnStringOrElse(title);
    }

    @Override
    public String getThumbnail() {
        return returnStringOrElse(europeanaObject);
    }

    @Override
    public String getCreator() {
        return returnStringOrElse(creator);
    }

    @Override
    public String getYear() {
        return returnStringOrElse(year);
    }

    @Override
    public String getProvider() {
        return returnStringOrElse(provider);
    }

    @Override
    public String getLanguage() {
        return returnStringOrElse(language);
    }

    @Override
    public DocType getType() {
        return DocType.get(docType);
    }

    @Override
    public BriefDocWindow getMoreLikeThis() {
        return null;  //Todo implement this
    }

}
