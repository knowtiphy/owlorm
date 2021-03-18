package org.knowtiphy.owlorm.javafx;

public class StoredPeer<S> extends Peer
{
	private final S storage;

	public StoredPeer(String id, S storage)
	{
		super(id);
		this.storage = storage;
	}

	public S getStorage()
	{
		return storage;
	}
}