package org.knowtiphy.owlorm.javafx;

import org.apache.jena.query.ResultSet;
import org.knowtiphy.babbage.storage.IStorage;
import org.knowtiphy.babbage.storage.exceptions.StorageException;
import org.knowtiphy.utils.Triple;

import java.util.Set;

public class StoredPeer extends Peer
{
	private final IStorage storage;

	public StoredPeer(String id, String type, IStorage storage)
	{
		super(id, type);
		this.storage = storage;
	}

	public IStorage getStorage()
	{
		return storage;
	}

	//	this one might be a bit too generic even for me :)
	protected Triple<Set<String>, Set<String>, Set<String>> diff(
			String containedType, Set<String> holding) throws StorageException
	{
		return QueryHelper.diff(storage, getId(), containedType, holding);
	}

	protected ResultSet getAttributes() throws StorageException
	{
		return QueryHelper.getAttributes(storage, getId(), getType());
	}
}