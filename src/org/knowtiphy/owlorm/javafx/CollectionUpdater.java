package org.knowtiphy.owlorm.javafx;

import java.util.function.Consumer;
import java.util.function.Function;
import javafx.collections.ObservableList;
import org.apache.jena.rdf.model.Statement;

/**
 * @author graham
 */
class CollectionUpdater<T> implements Consumer<Statement>
{

    private final ObservableList<T> list;
    private final Function<Statement, T> extractor;

    public CollectionUpdater(ObservableList<T> list, Function<Statement, T> extractor)
    {
        this.list = list;
        this.extractor = extractor;
    }

    @Override
    public void accept(Statement stmt)
    {
        list.add(extractor.apply(stmt));
    }
}