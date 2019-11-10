package org.knowtiphy.owlorm.javafx;

import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.jena.rdf.model.Statement;
import javafx.beans.property.Property;

/**
 * @author graham
 */
class PropertyUpdater<T> implements Consumer<Statement>
{

    private final Property<T> property;
    private final Function<Statement, T> extractor;

    public PropertyUpdater(Property<T> property, Function<Statement, T> extractor)
    {
        this.property = property;
        this.extractor = extractor;
    }

    @Override
    public void accept(Statement stmt)
    {
        property.setValue(extractor.apply(stmt));
    }
}