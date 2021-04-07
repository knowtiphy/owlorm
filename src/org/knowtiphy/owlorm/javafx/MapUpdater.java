package org.knowtiphy.owlorm.javafx;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import java.util.Map;
import java.util.function.Function;

/**
 * @author graham
 */
class MapUpdater<K, V> implements IUpdater
{
    private final Map<K, V> map;
    private final Function<Statement, K> key;
    private final Function<Statement, V> value;

    public MapUpdater(Map<K, V> map, Function<Statement, K> key, Function<Statement, V> value)
    {
        this.map = map;
        this.key = key;
        this.value = value;
    }

    @Override
    public void clear()
    {
        map.clear();
    }

    @Override
    public void accept(RDFNode node)
    {
        //  TODO
        //map.put(key.apply(stmt), value.apply(stmt));
    }
}