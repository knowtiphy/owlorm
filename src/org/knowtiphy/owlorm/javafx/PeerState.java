package org.knowtiphy.owlorm.javafx;

import javafx.application.Platform;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.knowtiphy.utils.JenaUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

//	TODO -- all this code should go away
public class PeerState
{
	private final static Map<String, IPeer> PEERS = new ConcurrentHashMap<>();
	private final static Map<String, Function<String, IPeer>> CONSTRUCTORS = new ConcurrentHashMap<>();

	public static IPeer peer(Resource resource)
	{
		IPeer peer = PEERS.get(resource.toString());
		assert peer != null : resource;
		return peer;
	}

	public static IPeer construct(String name, String type)
	{
		assert CONSTRUCTORS.containsKey(type);
		return construct(CONSTRUCTORS.get(type), name);
	}

	public static void addConstructor(String type, Function<String, IPeer> constructor)
	{
		assert !CONSTRUCTORS.containsKey(type);
		CONSTRUCTORS.put(type, constructor);
	}

	private static IPeer construct(Function<String, IPeer> constructor, String id)
	{
		assert !PEERS.containsKey(id) : id;
		IPeer peer = constructor.apply(id);
		PEERS.put(id, peer);
		return peer;
	}

	//	TODO -- all the code below here is crap and needs to go

	private static void apply(Consumer<Statement> change, Statement stmt)
	{
		//  ignore -- just means we don't have an updater/deleter for that property
		if (change != null)
		{
			Platform.runLater(() ->
			{
				try
				{
					change.accept(stmt);
				}
				catch (Exception ex)
				{
					ex.printStackTrace(System.err);
				}
			});
		}
	}

	private static void delete(IPeer peer, Statement stmt)
	{
		apply(peer.getDeleter(stmt.getPredicate().toString()), stmt);
	}

	//  introduce new peer objects by handling added triples of the form S rdf:type O
	private static void introduceNewPeers(Model added)
	{
		added.listStatements(null, added.createProperty(RDF.type.getURI()), (Resource) null)
				.forEachRemaining(stmt ->
				{
					var type = stmt.getObject().toString();
					var constructor = CONSTRUCTORS.get(type);
					if (constructor != null)
					{
						construct(constructor, stmt.getSubject().toString());
//						var root = ROOTS.get(type);
//						if (root != null)
//						{
//							root.accept(peer);
//						}
					}
				});
	}

	//	delete existing peers by handling deleted triples of the form X rdf:type Y
	private static void deleteExistingPeers(Model deleted)
	{
		deleted.listStatements(null, deleted.createProperty(RDF.type.getURI()), (Resource) null)
				.forEachRemaining(stmt -> Platform.runLater(() -> PEERS.remove(stmt.getSubject().toString())));
	}

	public static void update(IPeer peer, Statement stmt)
	{
		apply(peer.getUpdater(stmt.getPredicate().toString()), stmt);
	}
//
	//  process (delete/update-add) attributes of, or relationships between, existing peer objects

	private static void processExisting(Model model, Selector selector, BiConsumer<IPeer, Statement> process)
	{
		model.listStatements(selector).forEachRemaining(stmt ->
		{
			var subjectPeer = PEERS.get(stmt.getSubject().toString());
			if (subjectPeer != null)
			{
				process.accept(subjectPeer, stmt);
			}
		});
	}


	//	process an OWL model change -- constraints are that we must:
//	-	delete attributes of existing object before we update/add attribtes (since changes are deletes followed by adds)
//	-	introduce objects before we update an attribute as the attribute may be for the newly introduced objects.
//	So we choose the order
//  delete attributes of existing peer objects
//  delete relationships between existing peer objects
//	introduce new peers
//  update/add attributes of existing peer objects (which may have just been introduced)
//  update/add relationships between existing peer objects (which may have just been introduced)
//  delete existing peers

	public static void delta(Model added, Model deleted)
	{
		LOGGER.fine(() -> JenaUtils.toString(added));
		LOGGER.fine(() -> JenaUtils.toString(deleted));

		processExisting(deleted, DATA_PROPERTIES, PeerState::delete);
		processExisting(deleted, OBJECT_PROPERTIES, PeerState::delete);
		deleteExistingPeers(deleted);
		introduceNewPeers(added);
		//	which way around should we do the next two?
		processExisting(added, DATA_PROPERTIES, PeerState::update);
		processExisting(added, OBJECT_PROPERTIES, PeerState::update);
	}

	private static final Logger LOGGER = Logger.getLogger(PeerState.class.getName());

	private final static SimpleSelector DATA_PROPERTIES = new SimpleSelector(null, null, (RDFNode) null)
	{
		@Override
		public boolean test(Statement stmt)
		{
			return stmt.getObject().isLiteral();
		}
	};

	private final static SimpleSelector OBJECT_PROPERTIES = new SimpleSelector(null, null, (RDFNode) null)
	{
		@Override
		public boolean test(Statement stmt)
		{
			return !stmt.getObject().isLiteral() && !stmt.getPredicate().getURI().equals(RDF.type.getURI());
		}
	};
}



