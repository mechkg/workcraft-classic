package workcraft;

import org.w3c.dom.Element;

public interface XmlSerializable {
	public void fromXmlDom(Element element) throws DuplicateIdException;
	public Element toXmlDom(Element parent_element);
}