package org.knowtiphy.owlorm.javafx;

import javafx.collections.ObservableList;
import org.apache.jena.rdf.model.Statement;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author graham
 */
class CollectionUpdater<T> implements Consumer<Statement>
{
    private final ObservableList<T> list;
    private final Function<Statement, T> extractValue;

    public CollectionUpdater(ObservableList<T> list, Function<Statement, T> extractValue)
    {
        this.list = list;
        this.extractValue = extractValue;
    }

    @Override
    public void accept(Statement stmt)
    {
        list.add(extractValue.apply(stmt));
    }
}