package org.knowtiphy.owlorm.javafx;

import javafx.beans.property.BooleanProperty;

public interface IPeer extends IEntity
{
	BooleanProperty disabledProperty();
}