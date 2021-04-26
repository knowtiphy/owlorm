package org.knowtiphy.owlorm.javafx;

import org.apache.jena.query.ResultSet;
import org.knowtiphy.babbage.storage.IStorage;
import org.knowtiphy.babbage.storage.exceptions.StorageException;
import org.knowtiphy.utils.Triple;

import java.util.Set;

public interface IStoredPeer extends IEntity
{
	IStorage getStorage();

	ResultSet getAttributes() throws StorageException;

	void addUpdater(String predicate, IUpdater updater);

	void initialize(ResultSet rs);

	//	don't think this should really be here
	Triple<Set<String>, Set<String>, Set<String>> diff(
			String containedType, Set<String> holding) throws StorageException;

}