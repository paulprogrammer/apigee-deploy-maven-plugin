package io.apigee.buildTools.enterprise4g.utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A very naive threadlocal handler for document builders.
 * TODO: handle documentbuilder configurations
 * @author paul
 */
public class ThreadLocalDocumentBuilder extends ThreadLocal<DocumentBuilder> {

    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    @Override
    protected synchronized DocumentBuilder initialValue() {
        try {
            return dbf.newDocumentBuilder();
        } catch( Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DocumentBuilder get() {
        DocumentBuilder value = super.get();
        value.reset(); // always reset the builder to initial state.
        return value;
    }
}
