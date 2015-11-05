/**
 * Copyright (C) 2014 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apigee.buildTools.enterprise4g.utils;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import io.apigee.buildTools.enterprise4g.utils.ConfigTokens.Policy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * updates the configuration values of a package
 *
 * @author sdey
 */
public class PackageConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(PackageConfigurer.class);
    private static final ThreadLocalTransformerFactory tltf = new ThreadLocalTransformerFactory();

    private static int runReplacement(String env, ConfigTokens conf, String scope, List<File> toProcess) {
        int errorCount = 0;
        for( File xmlFile : toProcess) {
            try {
                Document xmlDoc = FileReader.getXMLDocument(xmlFile);
                Policy tokens;
                if(StringUtils.equalsIgnoreCase(scope,"proxy")) {
                    tokens = conf.getConfigbyEnv(env).getProxyFileNameMatch(xmlFile.getName());
                } else if(StringUtils.equalsIgnoreCase(scope,"policy")) {
                    tokens = conf.getConfigbyEnv(env).getPolicyFileNameMatch(xmlFile.getName());
                } else if(StringUtils.equalsIgnoreCase(scope,"target")) {
                    tokens = conf.getConfigbyEnv(env).getTargetFileNameMatch(xmlFile.getName());
                } else { continue; } // === N E X T ===
                xmlDoc = replaceTokens(xmlDoc, tokens);
                DOMSource source = new DOMSource(xmlDoc);
                StreamResult result = new StreamResult(xmlFile);
                tltf.get().transform(source, result);
            } catch( Exception e) {
                logger.error("Error processing " + xmlFile.getName(), e);
                errorCount++;
            }
        }

        return errorCount;
    }

    private static void doTokenReplacement( String env, ConfigTokens conf, Map<String,List<File>> toProcess) {
        int errorCount = 0;
        for(Map.Entry<String,List<File>> entry : toProcess.entrySet()) {
            errorCount += runReplacement(env, conf, entry.getKey(), entry.getValue());
        }

        if( errorCount > 0) {
            // some errors?  Whine & quit.
            logger.error(String.format("Encountered %d errors; see above.  Quitting", errorCount));
            throw new RuntimeException("Several errors encountered -- exiting"); // ** pouf **
        }
    }

    public static void configurePackage(String env, File configFile) throws Exception {
        Transformer transformer = tltf.get();

        // get the list of files in proxies folder
        XMLFileListUtil listFileUtil = new XMLFileListUtil();

        ConfigTokens conf = FileReader.getBundleConfigs(configFile);

        Map<String,List<File>> filesToProcess = new HashMap<String,List<File>>();
        filesToProcess.put("proxy",listFileUtil.getProxyFiles(configFile));
        filesToProcess.put("policy",listFileUtil.getPolicyFiles(configFile));
        filesToProcess.put("target",listFileUtil.getTargetFiles(configFile));

        doTokenReplacement(env, conf, filesToProcess);

        // special case ...
        File proxyFile = listFileUtil.getAPIProxyFiles(configFile).get(0);
        Document xmlDoc = FileReader.getXMLDocument(proxyFile); // there would be only one file, at least one file

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expression = xpath.compile("/APIProxy/Description");

        NodeList nodes = (NodeList) expression.evaluate(xmlDoc, XPathConstants.NODESET);
        if (nodes.item(0).hasChildNodes()) {
            // sets the description to whatever is in the <proxyname>.xml file
            nodes.item(0).setTextContent(expression.evaluate(xmlDoc));
        } else {
            // if Description is empty, then it reverts back to appending the username, git hash, etc
            nodes.item(0).setTextContent(getComment(proxyFile));
        }

        DOMSource source = new DOMSource(xmlDoc);
        StreamResult result = new StreamResult(proxyFile);
        transformer.transform(source, result);
    }

    public static Document replaceTokens(Document doc, Policy configTokens)
            throws XPathExpressionException, TransformerConfigurationException {

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(configTokens);
        logger.info(
                "============= to apply the following config tokens ================\n{}",
                json);

        try {
            for (int i = 0; i < configTokens.tokens.size(); i++) {

                logger.debug(
                        "=============Checking for Xpath Expressions {}  ================\n",
                        configTokens.tokens.get(i).xpath);
                XPathExpression expression = xpath
                        .compile(configTokens.tokens.get(i).xpath);

                NodeList nodes = (NodeList) expression.evaluate(doc,
                        XPathConstants.NODESET);

                for (int j = 0; j < nodes.getLength(); j++) {

                    if (nodes.item(j).hasChildNodes()) {
                        logger.debug(
                                "=============Updated existing value {} to new value {} ================\n",
                                nodes.item(j).getTextContent(),
                                configTokens.tokens.get(i).value);
                        nodes.item(j).setTextContent(
                                configTokens.tokens.get(i).value);
                    }
                }

            }

            return doc;
        } catch (XPathExpressionException e) {

            logger.error(String.format("\n\n=============The Xpath Expressions in config.json are incorrect. Please check. ================\n\n%s",
                    e.getMessage()), e);
            throw e;
        }

    }

    protected static String getComment(File basePath) {
        try {
            String hostname;
            String user = System.getProperty("user.name", "unknown");
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                hostname = InetAddress.getLocalHost().getHostAddress();
            }
            return user + " " + getScmRevision(basePath) + " " + hostname;
        } catch (Throwable t) {
            // If this blows up, continue on....
            return "";
        }
    }

    protected static String getScmRevision(File basePath ) {
        String rev;
        try {
            GitUtil gu = new GitUtil(basePath);
            String tagName = gu.getTagNameForWorkspaceHeadRevision();
            rev = "git: ";
            rev = (tagName == null) ? rev + "" : rev + tagName + " - ";
            String revNum = gu.getWorkspaceHeadRevisionString();
            revNum = revNum.substring(0, Math.min(revNum.length(), 8));
            rev = rev + revNum ;
        } catch (Throwable e) {
            rev = "git revision unknown";
        }

        return rev;
    }

}