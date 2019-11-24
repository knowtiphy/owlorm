package org.knowtiphy.owlorm.javafx;

import javafx.beans.property.BooleanProperty;
import org.apache.jena.rdf.model.Statement;

import java.util.function.Consumer;

public interface IPeer extends IEntity
{
	BooleanProperty disableProperty();

	Consumer<Statement> getUpdater(String attribute);

	Consumer<Statement> getDeleter(String attribute);
}