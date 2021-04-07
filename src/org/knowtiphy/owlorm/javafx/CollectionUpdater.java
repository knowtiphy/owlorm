package org.knowtiphy.owlorm.javafx;

import javafx.collections.ObservableList;
import org.apache.jena.rdf.model.RDFNode;

import java.util.function.Function;

/**
 * @author graham
 */
class CollectionUpdater<T> implements IUpdater
{
	private final ObservableList<T> collection;
	private final Function<RDFNode, T> extractValue;

	public CollectionUpdater(ObservableList<T> collection, Function<RDFNode, T> extractValue)
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