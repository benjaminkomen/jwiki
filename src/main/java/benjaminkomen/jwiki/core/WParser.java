package benjaminkomen.jwiki.core;

import benjaminkomen.jwiki.util.FL;
import benjaminkomen.jwiki.util.GSONP;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.util.*;

/**
 * Parses wikitext into a DOM-style, manipulatable format that is easy to work with.
 *
 * @author Fastily
 */
public class WParser {

    private static final Logger LOG = LoggerFactory.getLogger(WParser.class);
    private static final String VAR_TEMPLATE = "template";

    private WParser() {
        // no-args constructor
    }

    /**
     * Runs a parse query for wikitext/pages and then parses the result into a WikiText object.
     *
     * @param wiki        The Wiki object to use
     * @param queryParams Parameters to POST to the server
     * @return A WikiText object, or null on error.
     */
    private static WikiText parse(Wiki wiki, Map<String, String> queryParams) {
        queryParams.put("prop", "parsetree");
        try {
            XMLEventReader r = XMLInputFactory.newInstance()
                    .createXMLEventReader(new StringReader(GSONP
                            .getString(GSONP.getNestedJsonObject(JsonParser.parseString(wiki.basicPOST("parse", queryParams).body().string()).getAsJsonObject(),
                                    FL.toStringArrayList("parse", "parsetree")), "*")));

            WikiText root = new WikiText();
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();

                if (e.isStartElement() && nameIs(e.asStartElement(), VAR_TEMPLATE)) {
                    root.append(parseTemplate(r, root));
                } else if (e.isCharacters()) {
                    root.append(cToStr(e));
                }
            }
            return root;
        } catch (Exception e) {
            LOG.error("Error during parsing query", e);
            return null;
        }
    }

    /**
     * Parses the text of a page into a WikiText object.
     *
     * @param wiki The Wiki to use
     * @param page The title of the page to parse.
     * @return A WikiText representation of {@code page}, or null on error.
     */
    public static WikiText parsePage(Wiki wiki, String page) {
        return parse(wiki, FL.produceMap("page", page));
    }

    /**
     * Parses the text of a page into a WikiText object.
     *
     * @param wiki The Wiki to use
     * @param text The wikitext to parse
     * @return A WikiText representation of {@code text}, or null on error.
     */
    public static WikiText parseText(Wiki wiki, String text) {
        return parse(wiki, FL.produceMap("text", text, "contentmodel", "wikitext"));
    }

    /**
     * Parses a template. This function is to be called upon encountering a {@code template} StartElement.
     *
     * @param r      The XMLEventReader to use.
     * @param parent The parent WikiText the resulting WTemplate is to belong to, if applicable. Set null to disable.
     * @return The parsed WTemplate.
     * @throws Exception On parse error.
     */
    private static WTemplate parseTemplate(XMLEventReader r, WikiText parent) throws Exception {
        WTemplate t = new WTemplate(parent);

        String lastNameParsed = "";
        while (r.hasNext()) {
            XMLEvent e = r.nextEvent();
            if (e.isStartElement()) {
                StartElement se = e.asStartElement();
                switch (se.getName().getLocalPart()) {
                    case "title":
                        t.title = getNextElementText(r).strip();
                        break;
                    case "name":
                        Attribute index = se.getAttributeByName(new QName("index"));
                        lastNameParsed = index != null ? index.getValue() : getNextElementText(r).strip();
                        break;
                    case "equals":
                        getNextElementText(r);
                        break;
                    case "value":
                        t.put(lastNameParsed, parseTValue(r));
                        break;
                    default:
                        // do nothing - skip part tags
                }
            } else if (e.isEndElement() && nameIs(e.asEndElement(), VAR_TEMPLATE))
                break;
        }
        return t;
    }

    /**
     * Parses a template parameter value. PRECONDITION: the next element in {@code r} is of type Characters or a
     * StartElement for a new template.
     *
     * @param r The XMLEventReader to use.
     * @return WikiText representing this template's parameter value.
     * @throws Exception On parse error.
     */
    private static WikiText parseTValue(XMLEventReader r) throws Exception {
        WikiText root = new WikiText();

        while (r.hasNext()) {
            XMLEvent e = r.nextEvent();

            if (e.isStartElement() && nameIs(e.asStartElement(), VAR_TEMPLATE)) {
                root.append(parseTemplate(r, root));
            } else if (e.isCharacters()) {
                root.append(cToStr(e));
            } else if (e.isEndElement() && nameIs(e.asEndElement(), "value")) {
                break;
            }
        }
        return root;
    }

    /**
     * Gets the next Characters event(s) contained by the next pair of XMLEvent objects. Useful because sometimes a pair
     * of XML elements may be followed by more than one Characters event.
     *
     * @param r An XMLEventReader where the next XMLEvent object(s) is/are a Characters event(s).
     * @return The Strings of the Characters events as a String.
     * @throws Exception On parse error.
     */
    private static String getNextElementText(XMLEventReader r) throws Exception {
        StringBuilder result = new StringBuilder();

        while (r.hasNext()) {
            XMLEvent e = r.nextEvent();

            if (e.isStartElement()) {
                getNextElementText(r); // skip nested blocks, these are usually strangely placed comments
            } else if (e.isCharacters()) {
                result.append(cToStr(e));
            } else if (e.isEndElement()) {
                break;
            }
        }

        return result.toString();
    }

    /**
     * Converts a Characters XMLEvent to a String.
     *
     * @param e The XMLEvent (a Characters object) to convert to a String.
     * @return {@code e} as a String
     */
    private static String cToStr(XMLEvent e) {
        return e.asCharacters().getData();
    }

    /**
     * Check if the name portion of StartElement {@code e} is equal to String {@code n}
     *
     * @param e The StartElement to check
     * @param n The String to check against
     * @return True if {@code e} is the same as {@code n}
     */
    private static boolean nameIs(StartElement e, String n) {
        return e.getName().getLocalPart().equals(n);
    }

    /**
     * Check if the name portion of EndElement {@code e} is equal to String {@code n}
     *
     * @param e The EndElement to check
     * @param n The String to check against
     * @return True if {@code e} is the same as {@code n}
     */
    private static boolean nameIs(EndElement e, String n) {
        return e.getName().getLocalPart().equals(n);
    }

    /**
     * Mutable representation of parsed wikitext. May contain Strings and templates.
     *
     * @author Fastily
     */
    public static class WikiText {

        /**
         * Data structure backing wikitext storage.
         */
        protected Deque<Object> wikiTextStorage = new ArrayDeque<>();

        /**
         * Creates a new WikiText object
         *
         * @param objects Any Objects to pre-load this WikiText object with. Acceptable values are of type String or
         *                WTemplate.
         */
        public WikiText(Object... objects) {
            Arrays.stream(objects).forEach(this::append);
        }

        /**
         * Appends an Object to this WikiText object.
         *
         * @param o The Object to append. Acceptable values are of type String or WTemplate.
         */
        public void append(Object o) {
            if (o instanceof String) {
                wikiTextStorage.add((wikiTextStorage.peekLast() instanceof String ? wikiTextStorage.pollLast().toString() : "") + o);
            } else if (o instanceof WTemplate) {
                WTemplate t = (WTemplate) o;
                t.parent = this;
                wikiTextStorage.add(o);
            } else {
                throw new IllegalArgumentException("What is '" + o + "' ?");
            }
        }

        /**
         * Find top-level WTemplates contained by this WikiText
         *
         * @return A List of top-level WTemplates in this WikiText.
         */
        public List<WTemplate> getTemplates() {
            return FL.toArrayList(wikiTextStorage.stream()
                    .filter(WTemplate.class::isInstance)
                    .map(WTemplate.class::cast)
            );
        }

        /**
         * Recursively finds WTemplate objects contained by this WikiText.
         *
         * @return A List of all WTemplate objects in this WikiText.
         */
        public List<WTemplate> getTemplatesR() {
            List<WTemplate> wtl = new ArrayList<>();
            getTemplatesR(wtl);

            return wtl;
        }

        /**
         * Recursively finds WTemplate objects contained by this WikiText.
         *
         * @param wtl Any WTemplate objects found will be added to this List.
         * @see #getTemplatesR()
         */
        private void getTemplatesR(List<WTemplate> wtl) {
            wikiTextStorage.stream()
                    .filter(WTemplate.class::isInstance)
                    .map(WTemplate.class::cast)
                    .forEach(t -> {
                        t.params.values().forEach(wt -> wt.getTemplatesR(wtl));
                        wtl.add(t);
                    });
        }

        /**
         * Render this WikiText object as a String. Trims whitespace by default.
         */
        public String toString() {
            return toString(true);
        }

        /**
         * Render this WikiText as a String.
         *
         * @param doTrim If true, then trim whitespace.
         * @return A String representation of this WikiText.
         */
        public String toString(boolean doTrim) {
            StringBuilder b = new StringBuilder();
            for (Object o : wikiTextStorage) {
                b.append(o);
            }

            String out = b.toString();
            return doTrim ? out.strip() : out;
        }
    }

    /**
     * Mutable representation of a parsed wikitext template.
     *
     * @author Fastily
     */
    @Getter
    public static class WTemplate {
        /**
         * The parent WikiText object, if necessary
         */
        private WikiText parent;

        /**
         * This WTemplate's title
         */
        private String title = "";

        /**
         * The Map tracking this object's parameters.
         */
        private Map<String, WikiText> params = new LinkedHashMap<>();

        /**
         * Creates a new, empty WTemplate object.
         */
        public WTemplate() {
            this(null);
        }

        /**
         * Creates a new WTemplate with a parent.
         *
         * @param parent The parent WikiText object this WTemplate belongs to.
         */
        protected WTemplate(WikiText parent) {
            this.parent = parent;
        }

        /**
         * Normalize the title of the WTemplate, according to {@code wiki}. In other words, remove the 'Template:' namespace,
         * convert, capitalize the first letter, convert underscores to spaces.
         * <p>
         * TODO: Account for non-template NS
         *
         * @param wiki The Wiki to normalize against.
         */
        public void normalizeTitle(Wiki wiki) {
            if (wiki.whichNS(title).equals(NS.TEMPLATE)) {
                title = wiki.nss(title);
            }

            title = title.length() <= 1 ? title.toUpperCase() : "" + Character.toUpperCase(title.charAt(0)) + title.substring(1);
            title = title.replace('_', ' ');
        }

        /**
         * Test if the specified key {@code key} exists in this WTemplate. This does not check whether the parameter is empty
         * or not.
         *
         * @param key The key to check
         * @return True if there is a mapping for {@code key} in this WTemplate.
         */
        public boolean has(String key) {
            return params.containsKey(key) && !params.get(key).wikiTextStorage.isEmpty();
        }

        /**
         * Gets the specified WikiText value associated with key {@code key} in this WTemplate.
         *
         * @param key The key to get WikiText for.
         * @return The WikiText, or null if there is no mapping for {@code key}
         */
        public WikiText get(String key) {
            return params.get(key);
        }

        /**
         * Puts a new parameter in this Template.
         *
         * @param key   The name of the parameter
         * @param value The value of the parameter; acceptable types are WikiText, String, and WTemplate.
         */
        public void put(String key, Object value) {
            if (value instanceof WikiText) {
                params.put(key, (WikiText) value);
            } else if (value instanceof String || value instanceof WTemplate) {
                params.put(key, new WikiText(value));
            } else {
                throw new IllegalArgumentException(String.format("'%s' is not an acceptable type", value));
            }
        }

        /**
         * Appends {@code objectToAppend} to the end of the WikiText associated with {@code key}
         *
         * @param key            The key to associate new text with.
         * @param objectToAppend The Object to append to the value keyed by {@code key} in this WTemplate
         */
        public void append(String key, Object objectToAppend) {
            if (has(key)) {
                params.get(key).append(objectToAppend);
            } else {
                put(key, objectToAppend);
            }
        }

        /**
         * Removes the mapping for the specified key, {@code key}
         *
         * @param key Removes the mapping for this key, if possible
         */
        public void remove(String key) {
            params.remove(key);
        }

        /**
         * Removes this WTemplate from its parent WikiText object, if applicable.
         */
        public void drop() {
            if (parent == null) {
                return;
            }

            parent.wikiTextStorage.remove(this);
            parent = null;
        }

        /**
         * Re-map the a key to a new name.
         *
         * @param oldKey The old name
         * @param newKey The new name
         */
        public void remap(String oldKey, String newKey) {
            params.put(newKey, params.remove(oldKey));
        }

        /**
         * Get the keyset (all parameters) for this WTemplate. The resulting keyset does not back the internal Map.
         *
         * @return The keyset for this WTemplate.
         */
        public Set<String> keySet() {
            return new HashSet<>(params.keySet());
        }

        /**
         * Generates a String (wikitext) representation of this Template.
         *
         * @param indent Set true to add a newline between each parameter.
         * @return A String representation of this Template.
         */
        public String toString(boolean indent) {
            String base = (indent ? "%n" : "") + "|%s=%s";

            StringBuilder x = new StringBuilder();
            for (Map.Entry<String, WikiText> e : params.entrySet()) {
                x.append(String.format(base, e.getKey(), e.getValue()));
            }

            if (indent) {
                x.append("\n");
            }

            return String.format("{{%s%s}}", title, x.toString());
        }

        /**
         * Renders this WTemplate as a String.
         */
        public String toString() {
            return toString(false);
        }
    }
}