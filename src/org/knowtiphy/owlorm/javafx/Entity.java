package org.knowtiphy.owlorm.javafx;

import java.util.Objects;

public class Entity implements IEntity
{
	private final String id;

	public Entity(String id)
	{
		this.id = id;
	}

	@Override
	public String getId()
	{
		return id;
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
		return Objects.equals(this.id, other.id);
	}

	@Override
	public String toString()
	{
		return "Entity{" +
				"id='" + id + '\'' +
				'}';
	}
}