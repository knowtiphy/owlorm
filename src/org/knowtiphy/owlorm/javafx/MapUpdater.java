package org.knowtiphy.owlorm.javafx;

import org.apache.jena.rdf.model.Statement;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author graham
 */
class MapUpdater<K, V> implements Consumer<Statement>
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
    public void accept(Statement stmt)
    {
        map.put(key.apply(stmt), value.apply(stmt));
    }
}