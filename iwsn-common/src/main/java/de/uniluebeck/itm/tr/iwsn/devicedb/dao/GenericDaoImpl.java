package de.uniluebeck.itm.tr.iwsn.devicedb.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

/**
 * Extensions of this class provide functionality to manage
 * and persist entities in and from a data base by using an {@link EntityManager}
 * @author Sebastian Ebers
 *
 * @param <T> Type of entity to be persisted
 * @param <K> Type of the identifying primary key
 */
public abstract class GenericDaoImpl<T, K extends Serializable> implements GenericDao<T, K> {

	/** The instance which logs messages. */
	private static final Logger log = LoggerFactory.getLogger(GenericDaoImpl.class);

	/** The interface used to interact with the persistence context. */
	@Inject
	private EntityManager entityManager;

	/** The class of the managed and persistent entities. */
	private Class<T> entityClass;
	
	
	/**
	 * Constructor
	 * 
	 * @param typeclass
	 *            The class of the managed and persistent entities.
	 */
	public GenericDaoImpl(final Class<T> typeclass) {
		this.entityClass = typeclass;
	}
	
	/**
	 * Constructor
	 * 
	 * @param entityManager
	 *            The instance used to interact with the persistence context.
	 */
	@Inject
	public GenericDaoImpl(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}
	
	/**
	 * Constructor
	 * 
	 * @param entityManager
	 *            The instance used to interact with the persistence context.
	 * @param typeclass
	 *            The class of the managed and persistent entities.
	 */
	public GenericDaoImpl(final EntityManager entityManager, final Class<T> typeclass) {
		this.entityManager = entityManager;
		this.entityClass = typeclass;
	}


	/**
	 * Constructor inferring and setting the type class of the managed and persistent entities
	 */
	@SuppressWarnings("unchecked")
	public GenericDaoImpl() {
		Type superclass = getClass().getGenericSuperclass();
		if (superclass instanceof Class) {
			throw new RuntimeException("Missing type parameter.");
		}
		Type type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
		entityClass = (Class<T>) type;
	}

	/**
	 * Returns the interface used to interact with the persistence context.
	 * 
	 * @return the the interface used to interact with the persistence context.
	 */
	public final EntityManager getEntityManager() {
		return entityManager;
	}

	/**
	 * Returns the class of the managed and persistent entities.
	 * 
	 * @return the class of the managed and persistent entities.
	 */
	protected final Class<T> getEntityClass() {
		return entityClass;
	}

	@Override
	public T find(K id) {
		log.debug("Trying to fetch " + entityClass + " instance with id: " + id);
		return entityManager.find(entityClass, id);
	}

	@Override
	public List<T> find() {
		log.debug("Trying to fetch all " + entityClass + " instances from persistence context...");
		CriteriaQuery<T> createQuery = entityManager.getEntityManagerFactory().getCriteriaBuilder().createQuery(entityClass);
		createQuery.from(entityClass);
		List<T> resultList = entityManager.createQuery(createQuery).getResultList();
		log.debug(resultList.size()+" entities of class " + entityClass + " fetched from persistence context");
		return resultList;
	}

	@Override
	@Transactional
	public void save(T entity) {
		log.debug("Persisting " + entity);
		entityManager.persist(entity);
	}

	@Override
	@Transactional
	public void update(T entity) {
		log.debug("Persisting all local changes of " + entity);
		entityManager.merge(entity);
	}

	@Override
	@Transactional
	public void delete(T entity) {
		log.debug("Removing " + entity+" from persistence context");
		entityManager.remove(entity);
	}

	@SuppressWarnings("unchecked")
	@Override
	public K getKey(T entity) {
		Session session = entityManager.unwrap(Session.class);
		return (K) session.getSessionFactory().getClassMetadata(entityClass).getIdentifier(entity, (SessionImplementor) session);
	}

	@Override
	public void refresh(T entity) {
		log.debug("Reseting all properties of " + entity+" from persistence context");
		entityManager.refresh(entity);
	}

	@Override
	public boolean contains(T entity) {
		log.debug("Cecking whether " + entity+" consists in persistence context");
		return entityManager.contains(entity);
	}

}