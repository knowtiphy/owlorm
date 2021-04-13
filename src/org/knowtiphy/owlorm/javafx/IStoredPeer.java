package org.knowtiphy.owlorm.javafx;

import org.apache.jena.query.ResultSet;
import org.knowtiphy.babbage.storage.IStorage;
import org.knowtiphy.babbage.storage.exceptions.StorageException;

public interface IStoredPeer extends IEntity
{
	IStorage getStorage();

	ResultSet getAttributes() throws StorageException;
}