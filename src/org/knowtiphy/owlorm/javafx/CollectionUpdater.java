package org.knowtiphy.owlorm.javafx;

import org.apache.jena.rdf.model.RDFNode;

import java.util.Collection;
import java.util.function.Function;

/**
 * @author graham
 */
class CollectionUpdater<T> implements IUpdater
{
	private final Collection<T> collection;
	private final Function<RDFNode, T> extractValue;

	public CollectionUpdater(Collection<T> collection, Function<RDFNode, T> extractValue)
	{
		this.collection = collection;
		this.extractValue = extractValue;
	}

	@Override
	public void clear()
	{
		collection.clear();
	}

	@Override
	public void accept(RDFNode stmt)
	{
		collection.add(extractValue.apply(stmt));
	}
}