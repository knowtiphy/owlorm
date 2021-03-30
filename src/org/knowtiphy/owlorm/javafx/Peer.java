package org.knowtiphy.owlorm.javafx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
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
	private final SimpleBooleanProperty disabledProperty = new SimpleBooleanProperty(false);

	private final Map<String, Consumer<RDFNode>> updaters = new HashMap<>();

	public Peer(String id, String type)
	{
		super(id, type);
	}

	@Override
	public BooleanProperty disabledProperty()
	{
		return disabledProperty;
	}

	public void declareU(String predicate, Consumer<RDFNode> consumer)
	{
		updaters.put(predicate, consumer);
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

	public void initialize(ResultSet rs, String property)
	{
		rs.forEachRemaining((soln) -> initialize(soln, property));
	}

	public void initialize(ResultSet rs)
	{
		rs.forEachRemaining(this::initialize);
	}

	public static void disable(Collection<IPeer> peers)
	{
		peers.forEach(peer -> peer.disabledProperty().set(true));
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

	private void initialize(QuerySolution it)
	{
		initialize(it, it.get("p").toString());
	}
}


//	public void declareU(String predicate, ObservableList<String> set)
//	{
//		declareU(predicate, new CollectionUpdater<>(set, JenaUtils::getS));
//	}

//	public <K, V> void declareU(String predicate, Map<K, V> map,
//								Function<Statement, K> key, Function<Statement, V> value)
//	{
//		peerUpdater.put(predicate, new MapUpdater<>(map, key, value));
//	}
//public <T extends IEntity> void declareOU(String predicate, ObservableList<T> list)
//{
////		peerUpdater.put(predicate, new CollectionUpdater<>(list,
////				stmt -> {
////					//noinspection unchecked
////					T obj = (T) PeerState.peer(stmt.getObject().asResource());
////					assert !list.contains(obj) : obj.getId();
////					return obj;
////				}));
//
//}
//
//	public <T> void declareD(String predicate, ObservableList<T> set)
//	{
//		//noinspection unchecked
//		//peerDeleter.put(predicate, new CollectionDeleter<>(set, s -> (T) PeerState.peer(s.getObject().asResource())));
//	}

//
//	public void declareU(String predicate, BooleanProperty property)
//	{
//		declareU(predicate, new PropertyUpdater<>(property, JenaUtils::getB));
//	}
//	public void declareU(String predicate, ObjectProperty<ZonedDateTime> property)
//	{
//		declareU(predicate, new PropertyUpdater<>(property, JenaUtils::getDate));
//	}
//	public <T> void declareU(String predicate, ObservableList<T> set, Function<Statement, T> f)
//	{
//		declareU(predicate, new CollectionUpdater<>(set, f));
//	}

//	private void declareU(String predicate, Consumer<Statement> updater)
//	{
//		peerUpdater.put(predicate, updater);
//	}
//
//
//	public void declareU(String predicate, IntegerProperty property)
//	{
//		declareU(predicate, new PropertyUpdater<>(property, JenaUtils::getI));
//	}
//
//	public void declareU(String predicate, StringProperty property)
//	{
//		declareU(predicate, new PropertyUpdater<>(property, JenaUtils::getS));
//	}
//
//	public <T> void declareD(String predicate, ObservableList<T> set, Function<Statement, T> f)
//	{
//		peerDeleter.put(predicate, new CollectionDeleter<>(set, f));
//	}
//public void initialize(Model model)
//{
//	//	 - fudge to allow new updaters to use results sets rather than models
//	model.listStatements().forEachRemaining(s ->
//	{
//		var updater = getUpdater(s.getPredicate().toString());
//		if (updater != null)
//		{
//			apply(updater, s);
//		}
//	});
//}

//	@Override
//	public Consumer<Statement> getUpdater(String attribute)
//	{
//		return peerUpdater.get(attribute);
//	}


//	//	apply a change
//	//	note: this doesn't do any later{} calls -- callers of this have to manage UI thread issues
//	private void apply(Consumer<Statement> change, Statement stmt)
//	{
//		//  ignore -- just means we don't have an updater/deleter for that property
//		if (change != null)
//		{
//			try
//			{
//				change.accept(stmt);
//			}
//			catch (Exception ex)
//			{
//				//	 -- how do we handle this?
//				ex.printStackTrace(System.err);
//			}
//		}
//	}
//private final Map<String, Consumer<Statement>> peerUpdater = new HashMap<>();

//	@Override
//	public Consumer<Statement> getDeleter(String attribute)
//	{
//		return peerDeleter.get(attribute);
//	}
//
//private final Map<String, Consumer<Statement>> peerDeleter = new HashMap<>();

//		var updater = getUpdater(it.get("p").toString());
//		if (updater == null)
//		{
//	its an old fashioned updater -- do it the old way
//		}
//		else
//		{
//			apply(updater, it);
//		}