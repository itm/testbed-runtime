package de.uniluebeck.itm.tr.snaa.shiro;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityNotFoundException;

/**
 * Implementation of this interface provide functionality to manage
 * and persist entities in and from a data base.
 * @author Sebastian Ebers
 *
 * @param <T> Type of entity to be persisted
 * @param <K> Type of the identifying primary key
 */
public interface GenericDao<T, K extends Serializable>
{
	/**
	 * Returns a managed and persistent entity.
	 * @param id The entity's identifying primary key.
	 * @return A managed and persistent entity.
	 */
    T find(K id);

    /**
     * Returns all managed and persistent entities of type T
     * @return a list of entities of type T 
     */
    List<T> find();

    /**
     * Make an entity instance managed and persistent.
     * @param entity  entity instance
     */
    void save(T entity);
    
    /**
     * Update an entity's state in the current persistence context.
     * @param entity  entity instance
     * @throws IllegalArgumentException if not an entity
     */
    void update(T entity);

    /**
     * Remove the entity instance.
     * @param entity  entity instance
     * @throws IllegalArgumentException if the instance is not an
     *         entity or is a detached entity
     */
    void delete(T entity);
   
    /**
	 * Get the identifier of an instance (throw an exception if no identifier property)
	 *
	 * @param entity The entity for which to get the identifier
	 *
	 * @return The identifier
	 */
    K getKey(T entity);
    
    /**
     * Refresh the state of the instance from the database,
     * overwriting changes made to the entity, if any.
     * @param entity  entity instance
     * @throws IllegalArgumentException if the instance is not
     *         an entity or the entity is not managed
     * @throws EntityNotFoundException if the entity no longer
     *         exists in the database
     */
    void refresh(T entity);
    
    /**
     * Check if the instance is a managed entity instance belonging
     * to the current persistence context.
     * @param entity  entity instance
     * @return boolean indicating if entity is in persistence context
     * @throws IllegalArgumentException if not an entity
     */
    public boolean contains(T entity);
}
