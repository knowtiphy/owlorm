package org.knowtiphy.owlorm.javafx;

import javafx.collections.ObservableList;
import org.apache.jena.rdf.model.RDFNode;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author graham
 */
class CollectionUpdater<T> implements Consumer<RDFNode>
{
    private final ObservableList<T> list;
    private final Function<RDFNode, T> extractValue;

    public CollectionUpdater(ObservableList<T> list, Function<RDFNode, T> extractValue)
    {
        this.list = list;
        this.extractValue = extractValue;
    }

    @Override
    public void accept(RDFNode stmt)
    {
        list.add(extractValue.apply(stmt));
    }
}