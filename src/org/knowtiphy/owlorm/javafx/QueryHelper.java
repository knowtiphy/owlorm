package org.knowtiphy.owlorm.javafx;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.ResultSet;
import org.knowtiphy.babbage.storage.IStorage;
import org.knowtiphy.babbage.storage.Vocabulary;
import org.knowtiphy.babbage.storage.exceptions.StorageException;
import org.knowtiphy.utils.JenaUtils;
import org.knowtiphy.utils.Triple;

import java.util.HashSet;
import java.util.Set;

//  TODO -- this is a crappy name but ...
public class QueryHelper
{
	//	TODO -- not sure if the type part here is required, but makes the query tighter
	private static final ParameterizedSparqlString GET_IDS =
			new ParameterizedSparqlString("select * where { ?s <" + Vocabulary.CONTAINS + "> ?o. ?o a ?type}");

	private static final ParameterizedSparqlString GET_ATTRIBUTES =
			new ParameterizedSparqlString("select * where { ?s ?p ?o filter(?p != ?type) }");

	public static Triple<Set<String>, Set<String>, Set<String>> diff(IStorage storage, String id, String type, Set<String> holding) throws StorageException
	{
		//  compute the IDs stored on the server

		GET_IDS.setIri("s", id);
		GET_IDS.setIri("type", type);

		var stored = JenaUtils.collect(
				storage.query(GET_IDS.toString()),
				new HashSet<>(),
				(soln) -> soln.getResource("o").toString());

		//  work out what the added and deleted IDs are, and the IDs that possibly require updating

		var toAdd = new HashSet<String>();
		var toDelete = new HashSet<String>();
		var toUpdate = new HashSet<String>();

		stored.forEach(sid -> (holding.contains(sid) ? toUpdate : toAdd).add(sid));

		holding.forEach(hid -> {
			if (!stored.contains(hid))
			{
				toDelete.add(hid);
			}
		});

		return new Triple<>(toAdd, toDelete, toUpdate);
	}

	public static ResultSet getAttributes(IStorage storage, String id, String type) throws StorageException
	{
		GET_ATTRIBUTES.setIri("s", id);
		GET_ATTRIBUTES.setIri("type", type);
		return storage.query(GET_ATTRIBUTES.toString());
	}
}

//fun diff(storage : IStorage,
//         query : SelectBuilder,
//         id : String,
//         proj : String,
//         map : Map<String, *>) : Triple<Set<String>, Set<String>, Set<String>>
//{
//	val stored = HashSet<String>()
//
//	query.setVar(Var.alloc("id"), NodeFactory.createURI(id))
//	storage.query(query.buildString()).forEach { stored.add(it.getResource(proj).toString()) }
//
//	val toAdd = HashSet<String>()
//	val toDelete = HashSet<String>()
//	val toUpdate = HashSet<String>()
//
//	stored.forEach { if (!map.containsKey(it)) toAdd.add(it) else toUpdate.add(it) }
//	map.keys.forEach { if (!stored.contains(it)) toDelete.add(it) }
//
//	return Triple(toAdd, toDelete, toUpdate)
//}