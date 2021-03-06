/*
 * Copyright 2011 DELVING BV
 *
 *  Licensed under the EUPL, Version 1.0 or? as soon they
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
package eu.delving.core.util;

import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class ExposedKeysMessageSource extends ReloadableResourceBundleMessageSource {
    private Set<String> keySet = new TreeSet<String>();

    @Override
    public void setBasename(String basename) {
        setBasenames(new String[]{basename});
        PropertiesHolder holder = getProperties(basename);
        Properties properties = holder.getProperties();
        Enumeration<Object> keyEnumeration = properties.keys();
        while (keyEnumeration.hasMoreElements()) {
            keySet.add((String) keyEnumeration.nextElement());
        }
    }

    public Set<String> getKeySet() {
        return keySet;
    }
}
