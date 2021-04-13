package org.knowtiphy.owlorm.javafx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.knowtiphy.babbage.storage.IStorage;
import org.knowtiphy.babbage.storage.exceptions.StorageException;
import org.knowtiphy.utils.JenaUtils;
import org.knowtiphy.utils.Triple;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class StoredPeer extends Entity implements IStoredPeer
{
	private final IStorage storage;

	public StoredPeer(String uri, String type, IStorage storage)
	{
		super(uri, type);
		this.storage = storage;
	}

	public IStorage getStorage()
	{
		return storage;
	}

	private final Map<String, IUpdater> updaters = new HashMap<>();

	public void declareU(String predicate, IUpdater updater)
	{
		updaters.put(predicate, updater);
	}

	public void declareU(String predicate, BooleanProperty property)
	{
		declareU(predicate, new PropertyUpdater<>(property, JenaUtils::getB));
	}

	public void declareU(String predicate, IntegerProperty property)
	{
		declareU(predicate, new PropertyUpdater<>(property, JenaUtils::getI));
	}

	public void declareU(String predicate, StringProperty property)
	{
		declareU(predicate, new PropertyUpdater<>(property, JenaUtils::getS));
	}

	public void declareU(String predicate, ObjectProperty<ZonedDateTime> property)
	{
		declareU(predicate, new PropertyUpdater<>(property, JenaUtils::getDate));
	}

	public void declareU(String predicate, ObservableList<String> set)
	{
		declareU(predicate, new CollectionUpdater<>(set, JenaUtils::getS));
	}

	public <T> void declareU(String predicate, ObservableList<T> set, Function<RDFNode, T> f)
	{
		declareU(predicate, new CollectionUpdater<>(set, f));
	}

	private void apply(Consumer<RDFNode> change, QuerySolution soln)
	{
		//  ignore -- just means we don't have an updater/deleter for that property
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

	private void initialize(QuerySolution it, String property)
	{
		apply(updaters.get(property), it);
	}

	public void initialize(ResultSet rs, String property)
	{
		rs.forEachRemaining((soln) -> initialize(soln, property));
	}

	private void initialize(QuerySolution it, Set<String> first)
	{
		var property = it.get("p").toString();
		if(!first.contains(property))
		{
			first.add(property);
			//	TODO -- we look up the updater twice -- once in apply and once here ...
			IUpdater updater = updaters.get(property);
			if(updater != null)
				updater.clear();
		}
		initialize(it, property);
	}

	public void initialize(ResultSet rs)
	{
		var first = new HashSet<String>();
		rs.forEachRemaining((soln) -> initialize(soln, first));
	}

	//	this one might be a bit too generic even for me :)
	protected Triple<Set<String>, Set<String>, Set<String>> diff(
			String containedType, Set<String> holding) throws StorageException
	{
		return QueryHelper.diff(storage, getUri(), containedType, holding);
	}

	public ResultSet getAttributes() throws StorageException
	{
		return QueryHelper.getAttributes(storage, getUri(), getType());
	}
}