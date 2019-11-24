package org.knowtiphy.owlorm.javafx;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.knowtiphy.utils.JenaUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author graham
 */
public class Peer extends Entity implements IPeer
{

	private final static Map<String, IPeer> PEERS = new ConcurrentHashMap<>();
	private final static Map<String, Function<String, IPeer>> CONSTRUCTORS = new ConcurrentHashMap<>();
	private final static Map<String, Consumer<IPeer>> ROOTS = new ConcurrentHashMap<>();

	private final SimpleBooleanProperty disabled = new SimpleBooleanProperty(false);
	private final Map<String, Consumer<Statement>> peerUpdater, peerDeleter;

	//	TODO fix this
	@SuppressWarnings("LeakingThisInConstructor")
	public Peer(String id)
	{
		super(id);
		peerUpdater = new HashMap<>();
		peerDeleter = new HashMap<>();
		assert !PEERS.containsKey(id) : id + "::" + PEERS.get(id);
		PEERS.put(id, this);
	}

	public static IPeer peer(Resource resource)
	{
		IPeer peer = PEERS.get(resource.toString());
		assert peer != null : resource;
		return peer;
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

	@SuppressWarnings("unchecked")
	public void declareU(String predicate, IntegerProperty property)
	{
		peerUpdater.put(predicate, new PropertyUpdater(property, (Function<Statement, Integer>) JenaUtils::getI));
	}

	public void declareU(String predicate, ObjectProperty<LocalDate> property)
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

	public static void addConstructor(String type, Function<String, IPeer> constructor)
	{
		assert !CONSTRUCTORS.containsKey(type);
		CONSTRUCTORS.put(type, constructor);
	}

	public static void addRoot(String type, Consumer<IPeer> root)
	{
		assert !ROOTS.containsKey(type);
		ROOTS.put(type, root);
	}

	private final static SimpleSelector SELECT_DATA_PROPERTIES = new SimpleSelector(null, null, (RDFNode) null)
	{
		@Override
		public boolean test(Statement stmt)
		{
			return stmt.getObject().isLiteral();
		}
	};

	private final static SimpleSelector SELECT_OBJECT_PROPERTIES = new SimpleSelector(null, null, (RDFNode) null)
	{
		@Override
		public boolean test(Statement stmt)
		{
			return !stmt.getObject().isLiteral() && !stmt.getPredicate().getURI().equals(RDF.type.getURI());
		}
	};

	private final static SimpleSelector SELECT_NON_RDF_TYPE = new SimpleSelector(null, null, (RDFNode) null)
	{
		@Override
		public boolean test(Statement stmt)
		{
			return !stmt.getPredicate().getURI().equals(RDF.type.getURI());
		}
	};

	private static void update(Consumer<Statement> updater, Statement stmt)
	{
		if (updater != null)
		{
			Platform.runLater(() ->
			{
				try
				{
					updater.accept(stmt);
				} catch (RuntimeException ex)
				{
					ex.printStackTrace(System.err);
				}
			});
		}
	}

	private static void delete(Consumer<Statement> deleter, Statement stmt)
	{
		if (deleter != null)
		{
			Platform.runLater(() ->
			{
				try
				{
					deleter.accept(stmt);
				} catch (Exception ex)
				{
					ex.printStackTrace(System.err);
				}
			});
		}
	}

	//	process an OWL model change
	public static void delta(Model added, Model deleted, Predicate<Statement> predicate)
	{
		System.err.println("START  DELTA");
		JenaUtils.printModel(added, "+", predicate);
		JenaUtils.printModel(deleted, "-", predicate);

		//  create peer objects by handling added triples of the form X rdf:type Y
		var addPeers = added.listStatements(null, added.createProperty(RDF.type.getURI()), (Resource) null);

		while (addPeers.hasNext())
		{
			var stmt = addPeers.nextStatement();
			var subject = stmt.getSubject().toString();
			var object = stmt.getObject().toString();
			if (!PEERS.containsKey(subject))
			{
				var constructor = CONSTRUCTORS.get(object);
				if (constructor != null)
				{
					IPeer peer = constructor.apply(subject);
					PEERS.put(subject, peer);
					var root = ROOTS.get(object);
					if (root != null)
					{
						root.accept(peer);
					}
				}
			}
		}

		//  delete peer attributes and relationships by handling deleted triples of the
		//  form X Y Z, where Y is not rdf:type
		var deleteAttrRelationships = deleted.listStatements(SELECT_NON_RDF_TYPE);
		while (deleteAttrRelationships.hasNext())
		{
			var stmt = deleteAttrRelationships.nextStatement();
			var peerSubject = PEERS.get(stmt.getSubject().toString());
			if (peerSubject != null)
			{
				delete(peerSubject.getDeleter(stmt.getPredicate().toString()), stmt);
			}
		}

		//  initialize attributes of peers by handling added triples of the form X Y Z, where Z is a literal
		var peerAttributes = added.listStatements(SELECT_DATA_PROPERTIES);
		while (peerAttributes.hasNext())
		{
			var stmt = peerAttributes.nextStatement();
			var peerSubject = PEERS.get(stmt.getSubject().toString());
			if (peerSubject != null)
			{
				update(peerSubject.getUpdater(stmt.getPredicate().toString()), stmt);
			}
		}

		//  initialize relationships between peers by handling added triples of the form X Y Z,
		//  where Z is not a literal (so Y is an object property), but not rdf:type
		var peerRelationships = added.listStatements(SELECT_OBJECT_PROPERTIES);
		while (peerRelationships.hasNext())
		{
			var stmt = peerRelationships.nextStatement();
			var peerSubject = PEERS.get(stmt.getSubject().toString());
			if (peerSubject != null)
			{
				update(peerSubject.getUpdater(stmt.getPredicate().toString()), stmt);
			}
		}

		//  delete peers by handling deleted triples of the form X rdf:type Y
		var deletePeers = deleted.listStatements(null, deleted.createProperty(RDF.type.getURI()), (Resource) null);
		while (deletePeers.hasNext())
		{
			var stmt = deletePeers.nextStatement();
			Platform.runLater(() -> PEERS.remove(stmt.getSubject().toString()));
		}

		System.err.println("END  DELTA");
	}

	public static void delta(Model added, Model deleted)
	{
		delta(added, deleted, stmt -> false);
	}
}