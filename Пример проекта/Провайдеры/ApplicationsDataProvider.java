package com.fico.vtb.config;

import org.testng.annotations.DataProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ApplicationsDataProvider {
    private static final String PATH_TO_DATA_CONFIG = "/test_data.xml";
    private static Object[][] data;

    @DataProvider(parallel = false)
    public static Object[][] getAll() {
        init();
        return data;
    }

    private static void init() {
        if (data == null) {
            List<Object[]> fromXml = new ArrayList<Object[]>();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(ApplicationsDataProvider.class.getResourceAsStream(PATH_TO_DATA_CONFIG));
                NodeList suits = doc.getElementsByTagName("suite");
                for(int i=0; i < suits.getLength(); i++) {
                    String suitePath = suits.item(i).getTextContent();

                    Document suiteDoc = builder.parse(ApplicationsDataProvider.class.getResourceAsStream(suitePath));

                    NodeList applicationNodes = suiteDoc.getElementsByTagName("application");

                    for (int temp = 0; temp < applicationNodes.getLength(); temp++) {
                        Node applicationNode = applicationNodes.item(temp);
                        if (applicationNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element applicationNodeElement = (Element) applicationNode;
                            String path = null;
                            if (applicationNodeElement.getElementsByTagName("path").item(0) != null) {
                                path = applicationNodeElement.getElementsByTagName("path").item(0).getTextContent();
                            }
                            String test = applicationNodeElement.getElementsByTagName("test").item(0).getTextContent();

                            if (path != null) {
                                fromXml.add(new Object[]{test, "/" + path});
                            } else {
                                fromXml.add(new Object[]{test, null});
                            }
                        }
                    }
                }

                data = fromXml.toArray(new Object[fromXml.size()][2]);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
