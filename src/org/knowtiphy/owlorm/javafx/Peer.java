package org.knowtiphy.owlorm.javafx;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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

/**
 * @author graham
 */
public class Peer extends Entity implements IPeer
{

	public final static Map<String, IPeer> PEERS = new ConcurrentHashMap<>();
	public final static Map<String, Consumer<IPeer>> ROOTS = new ConcurrentHashMap<>();
	private final static Map<String, Function<String, IPeer>> CONSTRUCTORS = new ConcurrentHashMap<>();

	private final SimpleBooleanProperty disabled = new SimpleBooleanProperty(false);
	private final Map<String, Consumer<Statement>> updaters, deleters;

	@SuppressWarnings("LeakingThisInConstructor")
	public Peer(String id)
	{
		super(id);
		updaters = new HashMap<>();
		deleters = new HashMap<>();
		assert !PEERS.containsKey(id) : id + "::" + PEERS.get(id);
		PEERS.put(id, this);
	}

	@Override
	public BooleanProperty disableProperty()
	{
		return disabled;
	}

	public void declareU(String predicate, Consumer<Statement> updater)
	{
		updaters.put(predicate, updater);
	}

	public void declareU(String predicate, BooleanProperty property)
	{
		updaters.put(predicate, new PropertyUpdater<>(property, Functions.STMT_TO_BOOL));
	}

	public void declareU(String predicate, IntegerProperty property)
	{
		updaters.put(predicate, new PropertyUpdater(property, Functions.STMT_TO_INT));
	}

	public void declareU(String predicate, ObjectProperty<LocalDate> property)
	{
		updaters.put(predicate, new PropertyUpdater<>(property, Functions.STMT_TO_DATE));
	}

	public void declareU(String predicate, StringProperty property)
	{
		updaters.put(predicate, new PropertyUpdater<>(property, Functions.STMT_TO_STRING));
	}

	public <T> void declareU(String predicate, ObservableList<T> set, Function<Statement, T> f)
	{
		updaters.put(predicate, new CollectionUpdater<>(set, f));
	}

	public void declareU(String predicate, ObservableList<String> set)
	{
		updaters.put(predicate, new CollectionUpdater<>(set, Functions.STMT_TO_STRING));
	}

	public void declareD(String predicate, Consumer<Statement> deleter)
	{
		deleters.put(predicate, deleter);
	}

	@Override
	public Consumer<Statement> getUpdater(String attribute)
	{
		return updaters.get(attribute);
	}

	@Override
	public Consumer<Statement> getDeleter(String attribute)
	{
		return deleters.get(attribute);
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
					if(root != null)
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
			Platform.runLater(() ->
			{
				PEERS.remove(stmt.getSubject().toString());
			});
		}

		System.err.println("END  DELTA");
	}
}
