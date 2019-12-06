package org.knowtiphy.owlorm.javafx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.apache.jena.rdf.model.Statement;
import org.knowtiphy.utils.JenaUtils;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author graham
 */
public class Peer extends Entity implements IPeer
{
	private final SimpleBooleanProperty disabled = new SimpleBooleanProperty(false);
	private final Map<String, Consumer<Statement>> peerUpdater, peerDeleter;

	public Peer(String id)
	{
		super(id);
		peerUpdater = new HashMap<>();
		peerDeleter = new HashMap<>();
	}

	@Override
	public BooleanProperty disableProperty()
	{
		return disabled;
	}

	public void declareU(String predicate, Consumer<Statement> updater)
	{
		peerUpdater.put(predicate, updater);
	}

	public void declareU(String predicate, BooleanProperty property)
	{
		peerUpdater.put(predicate, new PropertyUpdater<>(property, JenaUtils::getB));
	}

	public void declareU(String predicate, IntegerProperty property)
	{
		//noinspection rawtypes,unchecked
		peerUpdater.put(predicate, new PropertyUpdater(property, (Function<Statement, Integer>) JenaUtils::getI));
	}

	public void declareU(String predicate, ObjectProperty<ZonedDateTime> property)
	{
		peerUpdater.put(predicate, new PropertyUpdater<>(property, JenaUtils::getLD));
	}

	public void declareU(String predicate, StringProperty property)
	{
		peerUpdater.put(predicate, new PropertyUpdater<>(property, JenaUtils::getS));
	}

	public <T> void declareU(String predicate, ObservableList<T> set, Function<Statement, T> f)
	{
		peerUpdater.put(predicate, new CollectionUpdater<>(set, f));
	}

	public void declareU(String predicate, ObservableList<String> set)
	{
		peerUpdater.put(predicate, new CollectionUpdater<>(set, JenaUtils::getS));
	}

	public <T extends IEntity> void declareOU(String predicate, ObservableList<T> list)
	{
		peerUpdater.put(predicate, new CollectionUpdater<>(list,
				stmt -> {
					//noinspection unchecked
					T obj = (T) PeerState.peer(stmt.getObject().asResource());
					assert !list.contains(obj) : obj.getId();
					return obj;
				}));

	}

	public <T> void declareD(String predicate, ObservableList<T> set, Function<Statement, T> f)
	{
		peerDeleter.put(predicate, new CollectionDeleter<>(set, f));
	}

	public <T> void declareD(String predicate, ObservableList<T> set)
	{
		//noinspection unchecked
		peerDeleter.put(predicate, new CollectionDeleter<>(set, s -> (T) PeerState.peer(s.getObject().asResource())));
	}

	public void declareD(String predicate, Consumer<Statement> deleter)
	{
		peerDeleter.put(predicate, deleter);
	}

	@Override
	public Consumer<Statement> getUpdater(String attribute)
	{
		return peerUpdater.get(attribute);
	}

	@Override
	public Consumer<Statement> getDeleter(String attribute)
	{
		return peerDeleter.get(attribute);
	}

	public static void disable(Collection<IPeer> peers)
	{
		for (IPeer peer : peers)
		{
			peer.disableProperty().set(true);
		}
	}
}