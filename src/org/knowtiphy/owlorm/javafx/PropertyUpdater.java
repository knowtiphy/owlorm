package org.knowtiphy.owlorm.javafx;

import javafx.beans.property.Property;
import org.apache.jena.rdf.model.RDFNode;

import java.util.function.Function;

/**
 * @author graham
 */
class PropertyUpdater<T> implements IUpdater
{
	private final Property<T> property;
	private final Function<RDFNode, T> extractValue;

	public PropertyUpdater(Property<T> property, Function<RDFNode, T> extractValue)
	{
		this.property = property;
		this.extractValue = extractValue;
	}

	@Override
	public void accept(RDFNode node)
	{
		property.setValue(extractValue.apply(node));
	}
}