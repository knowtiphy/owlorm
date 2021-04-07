package org.knowtiphy.owlorm.javafx;

import org.apache.jena.rdf.model.RDFNode;

import java.util.function.Consumer;

public interface IUpdater extends Consumer<RDFNode>
{
	//	this is a bit of a clumsy hack -- have to clear collection based peers before initializing them
	default void clear() {}
}
