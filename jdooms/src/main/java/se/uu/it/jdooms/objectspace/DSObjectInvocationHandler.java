package se.uu.it.jdooms.objectspace;

import org.apache.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Invocationhandler (deprecated)
 */
@Deprecated
public class DSObjectInvocationHandler implements InvocationHandler {
    private Object impl;
    private static final Logger logger = Logger.getLogger(DSObjectInvocationHandler.class);

    public DSObjectInvocationHandler(Object impl) {
        this.impl = impl;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        logger.debug("Method invoked");
        if(Object.class  == method.getDeclaringClass()) {
            String name = method.getName();
            if("equals".equals(name)) {
                return proxy == args[0];
            } else if("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            } else if("toString".equals(name)) {
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler " + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }
        return method.invoke(impl, args);
    }
}
