package org.knowtiphy.owlorm.javafx;

import java.util.Objects;

public class Entity implements IEntity
{
	private final String uri;
	private final String type;

	public Entity(String uri, String type)
	{
		this.uri = uri;
		this.type = type;
	}

	@Override
	public String getUri()
	{
		return uri;
	}

	public String getType()
	{
		return type;
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 23 * hash + Objects.hashCode(this.uri);
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
		//	two entities are equal of their uris are the same
		return Objects.equals(this.uri, other.uri);
	}

	@Override
	public String toString()
	{
		return "Entity{uri =" + uri + "}";
	}
}