package org.knowtiphy.owlorm.javafx;

import java.util.Objects;

public class Entity implements IEntity
{
	private final String id;
	private final String type;

	public Entity(String id, String type)
	{
		this.id = id;
		this.type = type;
	}

	@Override
	public String getId()
	{
		return id;
	}

	public String getType()
	{
		return type;
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 23 * hash + Objects.hashCode(this.id);
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final Entity other = (Entity) obj;
		//	two entities are equal of their ids are the same
		return Objects.equals(this.id, other.id);
	}

	@Override
	public String toString()
	{
		return "Entity{id=" + id + "}";
	}
}