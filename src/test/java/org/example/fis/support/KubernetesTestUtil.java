/*
 *  Copyright 2005-2023 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.example.fis.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KubernetesTestUtil {

    protected static Logger LOG = LoggerFactory.getLogger(KubernetesTestUtil.class);


    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrEmpty(Collection s) {
        return s == null || s.isEmpty();
    }


    public static boolean isNullOrEmpty(Object [] s) {
        return s == null || s.length == 0;
    }

    public static boolean isNotNullOrEmpty(String s) {
        return !isNullOrEmpty(s);
    }

    public static boolean isNotNullOrEmpty(Collection s) {
        return !isNullOrEmpty(s);
    }

    public static InputStream getResourceFileAsStream(String fileName) {
        return KubernetesTestUtil.class.getClassLoader().getResourceAsStream(fileName);
    }

    public static void failNotDeployed(){
        String errorMessage = "Error loading resource file. Be sure to run `mvn oc:deploy` before running this integration test.";
        LOG.error(errorMessage);
        throw new RuntimeException(errorMessage);
    }

    public static String getStringProperty(String name, Map<String, String> map) {
        return  getStringProperty(name, map, null);
    }
    public static String getStringProperty(String name, Map<String, String> map, String defaultValue) {
        if (map.containsKey(name) && isNotNullOrEmpty(map.get(name))) {
            defaultValue = map.get(name);
        }
        return defaultValue;
    }

    public static List<String> getArrayListStringProperty(String name, Map<String, String> map) {
        if (map.containsKey(name) && isNotNullOrEmpty(map.get(name))) {
            return Arrays.asList(map.get(name).split(","));
        }
        return null;
    }

    public static int getIntProperty(String name, Map<String, String> map, int defaultValue) {
        if (map.containsKey(name) && isNotNullOrEmpty(map.get(name))) {
            return Integer.parseInt(map.get(name));
        }
        return defaultValue;
    }

    public static Boolean getBooleanProperty(String name, Map<String, String> map, Boolean defaultValue) {
        if (map.containsKey(name) && isNotNullOrEmpty(map.get(name))) {
            defaultValue = Boolean.parseBoolean(map.get(name));
        }
        return defaultValue;
    }

    public static String getArtifactId() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);

        DocumentBuilder builder = domFactory.newDocumentBuilder();
        Document doc = builder.parse(new File("./pom.xml"));
        XPath xpath = XPathFactory.newInstance().newXPath();

        XPathExpression expr = xpath.compile("/*[local-name() = 'project']/*[local-name() = 'artifactId']/text()");
        Node result = (Node) expr.evaluate(doc, XPathConstants.NODE);
        String artifactId = result.getTextContent();
        LOG.info("Detected artifactId: {}", artifactId);
        return artifactId;
    }

}