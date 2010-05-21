package eu.europeana.sip.groovy;

import eu.europeana.sip.model.GlobalField;
import eu.europeana.sip.model.GlobalFieldModel;
import eu.europeana.sip.xml.MetadataRecord;
import groovy.lang.Binding;
import groovy.xml.MarkupBuilder;
import groovy.xml.NamespaceBuilder;

import java.io.Writer;

/**
 * Create a binding that we can use to execute snippets, with record that we can update.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class MappingScriptBinding extends Binding {
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    private static final String DC = "dc";
    private static final String DCTERMS = "dcterms";
    private static final String EUROPEANA = "europeana";

    public MappingScriptBinding(Writer writer, GlobalFieldModel model) {
        MarkupBuilder builder = new MarkupBuilder(writer);
        NamespaceBuilder xmlns = new NamespaceBuilder(builder);
        setVariable(OUTPUT, builder);
        setVariable(DC, xmlns.namespace("http://purl.org/dc/elements/1.1/", "dc"));
        setVariable(DCTERMS, xmlns.namespace("http://purl.org/dc/terms/", "dcterms"));
        setVariable(EUROPEANA, xmlns.namespace("http://www.europeana.eu/schemas/ese/", "europeana"));
        for (GlobalField globalField : GlobalField.values()) {
            setVariable(globalField.getVariableName(), model.get(globalField));
        }
    }

    public void setRecord(MetadataRecord record) {
        setVariable(INPUT, record.getRootNode());
    }
}
