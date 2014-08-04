package com.appdynamics.extensions.wmb;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class ParserBuilder {


    public Unmarshaller getParser(Class... classes) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(classes);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return jaxbUnmarshaller;
    }

}
