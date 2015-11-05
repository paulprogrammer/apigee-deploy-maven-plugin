package io.apigee.buildTools.enterprise4g.utils;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

/**
 * A naive thread local transfomer provider.
 * TODO: handle transformer factory configurations
 * @author Paul Williams (pwilliams@apigee.com)
 */
public class ThreadLocalTransformerFactory extends ThreadLocal<Transformer> {

    public static TransformerFactory tf = TransformerFactory.newInstance();

    @Override
    protected synchronized Transformer initialValue() {
        try {
            return tf.newTransformer();
        } catch( Exception e) {
            // what could POSSIBLY go wrong?
            throw new RuntimeException(e);
        }
    }
}
