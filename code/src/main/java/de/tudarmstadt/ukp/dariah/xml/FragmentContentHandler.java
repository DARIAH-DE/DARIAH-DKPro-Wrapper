package de.tudarmstadt.ukp.dariah.xml;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class FragmentContentHandler extends DefaultHandler {

    private String xPath = "/";
    private XMLReader xmlReader;
    private FragmentContentHandler parent;
    private StringBuilder characters = new StringBuilder();
    private Map<String, Integer> elementNameCount = new HashMap<String, Integer>();
    
    private LinkedList<String[]> textElements; // = new LinkedList<>();

    public FragmentContentHandler(XMLReader xmlReader, LinkedList<String[]> textElements) {
        this.xmlReader = xmlReader;
        this.textElements = textElements;
    }

    private FragmentContentHandler(String xPath, XMLReader xmlReader, FragmentContentHandler parent, LinkedList<String[]> textElements) {
        this(xmlReader, textElements);
        this.xPath = xPath;
        this.parent = parent;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        Integer count = elementNameCount.get(qName);
        if(null == count) {
            count = 1;
        } else {
            count++;
        }
        elementNameCount.put(qName, count);
        String childXPath = xPath + "/" + qName + "[" + count + "]";

        int attsLength = atts.getLength();
        for(int x=0; x<attsLength; x++) {
            //System.out.println(childXPath + "[@" + atts.getQName(x) + "='" + atts.getValue(x) + ']');
        }

        FragmentContentHandler child = new FragmentContentHandler(childXPath, xmlReader, this, textElements);
        xmlReader.setContentHandler(child);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String value = characters.toString().trim();
        if(value.length() > 0) {
//            System.out.println(xPath + "='" + value + "'");
        	textElements.add(new String[] {xPath, value });
        }
        xmlReader.setContentHandler(parent);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        characters.append(ch, start, length);
    }
    


}