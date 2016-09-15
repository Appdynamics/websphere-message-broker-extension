package com.appdynamics.extensions.wmb;


import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

public class XmlParser<T>{

    Unmarshaller unmarshaller;

    XmlParser(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    public T parse(String text) throws JAXBException {
        StringReader reader = new StringReader(text);
        return (T)unmarshaller.unmarshal(reader);
    }
}
