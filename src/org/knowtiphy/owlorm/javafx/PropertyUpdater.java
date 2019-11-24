package org.knowtiphy.owlorm.javafx;

import javafx.beans.property.Property;
import org.apache.jena.rdf.model.Statement;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author graham
 */
class PropertyUpdater<T> implements Consumer<Statement>
{
    private final Property<T> property;
    private final Function<Statement, T> extractValue;

    public PropertyUpdater(Property<T> property, Function<Statement, T> extractValue)
    {
        this.property = property;
        this.extractValue = extractValue;
    }

    @Override
    public void accept(Statement stmt)
    {
        property.setValue(extractValue.apply(stmt));
    }
}