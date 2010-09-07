package eu.europeana.sip.core;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maintain a map of input field values to output field values in such a way that it can
 * be written to the Groovy mapping code.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class ValueMapModel {
    private static final String PREFIX = "/* ValueMap */ def ";
    private static final String PREFIX_RANGE = "// ";
    private static final Pattern ENTRY_PATTERN = Pattern.compile("'([^']*)':'([^']*)'");
    private Map<String, ValueMap> maps = new TreeMap<String, ValueMap>();

    public static ValueMapModel fromMapping(String mapping) {
        ValueMapModel model = new ValueMapModel();
        ValueMap valueMap = null;
        for (String line : mapping.split("\n")) {
            if (line.startsWith(PREFIX)) {
                String def = line.substring(PREFIX.length());
                int eq = def.indexOf('=');
                if (eq < 0) throw new RuntimeException("No equals sign found");
                String name = def.substring(0, eq).trim();
                int range = def.indexOf(PREFIX_RANGE);
                if (range < 0) throw new RuntimeException("No range values");
                String rangeString = def.substring(range + PREFIX_RANGE.length());
                Set<String> rangeValues = new TreeSet<String>();
                rangeValues.addAll(Arrays.asList(rangeString.split(",")));
                valueMap = new ValueMap(name, rangeValues);
            }
            else if (valueMap != null) {
                if ("]".equals(line)) {
                    model.maps.put(valueMap.getName(), valueMap);
                    valueMap = null;
                }
                else {
                    Matcher matcher = ENTRY_PATTERN.matcher(line);
                    if (!matcher.matches()) {
                        throw new RuntimeException(String.format("Line [%s] does not match entry pattern", line));
                    }
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    valueMap.put(key, value);
                }
            }
        }
        return model;
    }

    public Map<String, ValueMap> getMaps() {
        return maps;
    }

    public String toString() {
        StringBuilder out = new StringBuilder();
        for (ValueMap valueMap : maps.values()) {
            out.append(valueMap.toString());
        }
        return out.toString();
    }

    public static class ValueMap {
        private String name;
        private Set<String> rangeValues;
        private Map<String, String> map = new TreeMap<String, String>();

        public ValueMap(String name, Set<String> rangeValues) {
            this.name = name;
            this.rangeValues = rangeValues;
        }

        public String getName() {
            return name;
        }

        public Set<String> getRangeValues() {
            return rangeValues;
        }

        public void put(String key, String value) {
            if (!rangeValues.contains(value)) {
                throw new RuntimeException(String.format("Value [%s] not among range values %s", value, rangeValues.toString()));
            }
            map.put(key, value);
        }

        public String get(String key) {
            return map.get(key);
        }

        public String toString() {
            StringBuilder out = new StringBuilder(PREFIX + name + " = [ // ");
            for (String rangeValue : rangeValues) {
                out.append(rangeValue).append(',');
            }
            out.append('\n');
            for (Map.Entry<String, String> entry : map.entrySet()) {
                out.append("'").append(entry.getKey()).append("':'").append(entry.getValue()).append("'\n");
            }
            out.append("]\n");
            return out.toString();
        }
    }
}
