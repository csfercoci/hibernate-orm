/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jaxb.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import org.hibernate.internal.util.xml.LocalXmlResourceResolver;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class LegacyJPAEventReader   extends EventReaderDelegate {
	private final XMLEventFactory xmlEventFactory;
	private final String namespaceUri;

	public LegacyJPAEventReader(XMLEventReader reader, String namespaceUri) {
		this( reader, XMLEventFactory.newInstance(), namespaceUri );
	}

	public LegacyJPAEventReader(XMLEventReader reader, XMLEventFactory xmlEventFactory, String namespaceUri) {
		super( reader );
		this.xmlEventFactory = xmlEventFactory;
		this.namespaceUri = namespaceUri;
	}

	private StartElement withNamespace(StartElement startElement) {
		// otherwise, wrap the start element event to provide a default namespace mapping
		final List<Namespace> namespaces = new ArrayList<Namespace>();
		namespaces.add( xmlEventFactory.createNamespace( "", namespaceUri ) );
		Iterator<?> originalNamespaces = startElement.getNamespaces();
		while ( originalNamespaces.hasNext() ) {
			Namespace ns = (Namespace) originalNamespaces.next();
			if ( !LocalXmlResourceResolver.INITIAL_JPA_ORM_NS.equals( ns.getNamespaceURI() ) ) {
				namespaces.add( ns );
			}
		}
		Iterator<?> attributes;
		if ( "entity-mappings".equals( startElement.getName().getLocalPart() ) ) {
			List st = new ArrayList();
			Iterator itr = startElement.getAttributes();
			while ( itr.hasNext() ) {
				Attribute obj = (Attribute) itr.next();
				if ( "version".equals( obj.getName().getLocalPart() ) ) {
					if ( "".equals( obj.getName().getPrefix() ) ) {
						st.add( xmlEventFactory.createAttribute( obj.getName(), "2.1" ) );
					}
				}
				else {
					st.add( obj );
				}
			}
			attributes = st.iterator();
		} else {
			attributes = startElement.getAttributes();
		}

		return xmlEventFactory.createStartElement(
				new QName( namespaceUri, startElement.getName().getLocalPart() ),
				attributes,
				namespaces.iterator()
		);
	}

	@Override
	public XMLEvent nextEvent() throws XMLStreamException {
		return wrap( super.nextEvent() );
	}

	private XMLEvent wrap(XMLEvent event) {
		if ( event.isStartElement() ) {
			return withNamespace( event.asStartElement() );
		}
		return event;
	}

	@Override
	public XMLEvent peek() throws XMLStreamException {
		return wrap( super.peek() );
	}
}
