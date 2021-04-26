package org.knowtiphy.owlorm.javafx;

import org.apache.jena.rdf.model.RDFNode;

import java.util.Map;
import java.util.function.Function;

/**
 * @author graham
 */
class MapUpdater<K, V> implements IUpdater
{
    private final Map<K, V> map;
    private final Function<RDFNode, K> key;
    private final Function<RDFNode, V> value;

    public MapUpdater(Map<K, V> map, Function<RDFNode, K> key, Function<RDFNode, V> value)
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
        map.put(key.apply(node), value.apply(node));
    }
}