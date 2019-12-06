package org.knowtiphy.owlorm.javafx;

import javafx.application.Platform;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.knowtiphy.utils.JenaUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class PeerState
{
	private static final Logger LOGGER = Logger.getLogger(PeerState.class.getName());

	private final static Map<String, IPeer> PEERS = new ConcurrentHashMap<>();
	private final static Map<String, Function<String, IPeer>> CONSTRUCTORS = new ConcurrentHashMap<>();
	private final static Map<String, Consumer<IPeer>> ROOTS = new ConcurrentHashMap<>();

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

	public static IPeer peer(Resource resource)
	{
		IPeer peer = PEERS.get(resource.toString());
		assert peer != null : resource;
		return peer;
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

	private static IPeer construct(Function<String, IPeer> constructor, String id)
	{
		assert !PEERS.containsKey(id) : id;
		IPeer peer = constructor.apply(id);
		PEERS.put(id, peer);
		return peer;
	}

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
		LOGGER.fine(() -> JenaUtils.toString("+", added, predicate));
		LOGGER.fine(() -> JenaUtils.toString("-", deleted, predicate));

		//JenaUtils.printModel(added, "+", predicate);
		//  create peer objects by handling added triples of the form X rdf:type Y
		var addPeers = added.listStatements(null, added.createProperty(RDF.type.getURI()), (Resource) null);

		while (addPeers.hasNext())
		{
			var stmt = addPeers.nextStatement();
			var peerId = stmt.getSubject().toString();
			var type = stmt.getObject().toString();

			var constructor = CONSTRUCTORS.get(type);
			if (constructor != null)
			{
				IPeer peer = construct(constructor, peerId);
				var root = ROOTS.get(type);
				if (root != null)
				{
					root.accept(peer);
				}
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

		//  delete peers by handling deleted triples of the form X rdf:type Y
		var deletePeers = deleted.listStatements(null, deleted.createProperty(RDF.type.getURI()), (Resource) null);
		while (deletePeers.hasNext())
		{
			var stmt = deletePeers.nextStatement();
			Platform.runLater(() -> PEERS.remove(stmt.getSubject().toString()));
		}
	}

	public static void delta(Model added, Model deleted)
	{
		delta(added, deleted, stmt -> true);
	}
}
