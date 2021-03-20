package org.knowtiphy.owlorm.javafx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.knowtiphy.utils.JenaUtils;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.knowtiphy.utils.JenaUtils.P;
import static org.knowtiphy.utils.JenaUtils.R;

/**
 * @author graham
 */
public class Peer extends Entity implements IPeer
{
	private final SimpleBooleanProperty disabled = new SimpleBooleanProperty(false);
	private final Map<String, Consumer<Statement>> peerUpdater= new HashMap<>();
	private final Map<String, Consumer<Statement>>  peerDeleter= new HashMap<>();

	public Peer(String id)
	{
		super(id);
	}

	@Override
	public BooleanProperty disableProperty()
	{
		return disabled;
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
		peerUpdater.put(predicate, new PropertyUpdater<>(property, JenaUtils::getI));
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

	public <K, V> void declareU(String predicate, Map<K,V> map,
								Function<Statement, K> key, Function<Statement, V> value)
	{
		peerUpdater.put(predicate, new MapUpdater<>(map, key, value));
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

	//	apply a change
	//	note: this doesn't do any later{} calls -- callers of this have to manage UI
	//	thread issues
	private void apply(Consumer<Statement> change, Statement stmt)
	{
		//  ignore -- just means we don't have an updater/deleter for that property
		if (change != null)
		{
			try
			{
				change.accept(stmt);
			}
			catch (Exception ex)
			{
				//	TODO -- how do we handle this?
				ex.printStackTrace(System.err);
			}
		}
	}

	public void initialize(Model model)
	{
		model.listStatements().forEachRemaining(s -> apply(getUpdater(s.getPredicate().toString()), s));
	}

	//	this is a hack but will do for the moment
	public void initialize(QuerySolution it)
	{
		Model model = ModelFactory.createDefaultModel();
		model.add(R(model ,getId()), P(model, it.get("p").toString()), it.get("o"));
		initialize(model);
	}

	public void initialize(ResultSet rs)
	{
		rs.forEachRemaining(this::initialize);
	}

	public static void disable(Collection<IPeer> peers)
	{
		peers.forEach(peer -> peer.disableProperty().set(true));
	}
}