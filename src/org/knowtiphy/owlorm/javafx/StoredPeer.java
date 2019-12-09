package org.knowtiphy.owlorm.javafx;

import org.knowtiphy.babbage.storage.IStorage;

public class StoredPeer extends Peer
{
	private IStorage storage;

	public StoredPeer(String id, IStorage storage)
	{
		super(id);
		this.storage = storage;
	}

	public IStorage getStorage()
	{
		return storage;
	}
}
