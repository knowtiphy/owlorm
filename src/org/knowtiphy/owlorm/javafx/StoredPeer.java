package org.knowtiphy.owlorm.javafx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.knowtiphy.babbage.storage.IStorage;
import org.knowtiphy.babbage.storage.exceptions.StorageException;
import org.knowtiphy.utils.JenaUtils;
import org.knowtiphy.utils.Triple;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class StoredPeer extends Entity implements IStoredPeer
{
	private final IStorage storage;
	private final Map<String, IUpdater> updaters;

	public StoredPeer(String uri, String type, IStorage storage)
	{
		super(uri, type);
		this.storage = storage;
		updaters = new HashMap<>();
	}

	public IStorage getStorage()
	{
		return storage;
	}

	public void addUpdater(String p, IUpdater updater)
	{
		updaters.put(p, updater);
	}

	public void addUpdater(String p, BooleanProperty property)
	{
		addUpdater(p, new PropertyUpdater<>(property, JenaUtils::getB));
	}

	public void addUpdater(String p, IntegerProperty property)
	{
		addUpdater(p, new PropertyUpdater<>(property, JenaUtils::getI));
	}

	public void addUpdater(String p, StringProperty property)
	{
		addUpdater(p, new PropertyUpdater<>(property, JenaUtils::getS));
	}

	public void addUpdater(String p, ObjectProperty<ZonedDateTime> property)
	{
		addUpdater(p, new PropertyUpdater<>(property, JenaUtils::getDT));
	}

	public <T> void addUpdater(String p, Collection<T> collection, Function<RDFNode, T> f)
	{
		addUpdater(p, new CollectionUpdater<>(collection, f));
	}

	//	apply an updater
	private void update(Consumer<RDFNode> change, QuerySolution soln)
	{
		//  ignore -- just means we don't have an updater for that property
		if (change != null)
		{
			try
			{
				change.accept(soln.get("o"));
			}
			catch (Exception ex)
			{
				//	TODO -- how do we handle this?
				ex.printStackTrace(System.err);
			}
		}
	}

	private void initialize(QuerySolution it, String p)
	{
		update(updaters.get(p), it);
	}

	public void initialize(ResultSet rs, String p)
	{
		rs.forEachRemaining((soln) -> initialize(soln, p));
	}

	private void initialize(QuerySolution it, Set<String> first)
	{
		var p = it.get("p").toString();
		if (!first.contains(p))
		{
			first.add(p);
			//	TODO -- we look up the updater twice -- once in apply and once here ...
			IUpdater updater = updaters.get(p);
			if (updater != null)
			{
				updater.clear();
			}
		}

		initialize(it, p);
	}

	public void initialize(ResultSet rs)
	{
		var first = new HashSet<String>();
		rs.forEachRemaining((soln) -> initialize(soln, first));
	}

	//	this one might be a bit too generic even for me :)
	public Triple<Set<String>, Set<String>, Set<String>> diff(
			String containedType, Set<String> holding) throws StorageException
	{
		return QueryHelper.diff(storage, getUri(), containedType, holding);
	}

	public ResultSet getAttributes() throws StorageException
	{
		return QueryHelper.getAttributes(storage, getUri(), getType());
	}
}