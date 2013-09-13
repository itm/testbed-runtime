package de.uniluebeck.itm.tr.devicedb.entity;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;

import javax.persistence.*;
import java.util.*;


@Entity(name="ChannelHandlerConfig")
@Cacheable
public class ChannelHandlerConfigEntity {
	
	private static final Function<ChannelHandlerConfig,ChannelHandlerConfigEntity> CHC_TO_ENTITY_FUNCTION =
			new Function<ChannelHandlerConfig, ChannelHandlerConfigEntity>() {
				@Override
				public ChannelHandlerConfigEntity apply(ChannelHandlerConfig config) {
					return new ChannelHandlerConfigEntity(config);
				}
			};
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
	
	private String handlerName;

	private String instanceName;

	@ManyToMany(cascade = CascadeType.ALL, fetch=FetchType.EAGER)
	private Set<KeyValueEntity> properties;
	
	public ChannelHandlerConfigEntity() {	}

	public ChannelHandlerConfigEntity(ChannelHandlerConfig config) {
		this.handlerName = config.getHandlerName();
		this.instanceName = config.getInstanceName();
		
		if ( config.getProperties() != null ) {
			this.properties = new HashSet<KeyValueEntity>();
			for ( String key : config.getProperties().keySet() ) {
				for ( String value : config.getProperties().get(key) ) {
					KeyValueEntity kv = new KeyValueEntity(key, value);
					this.properties.add(kv);
				}
			}
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getHandlerName() {
		return handlerName;
	}

	public void setHandlerName(String handlerName) {
		this.handlerName = handlerName;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public Set<KeyValueEntity> getProperties() {
		return properties;
	}

	public void setProperties(Set<KeyValueEntity> properties) {
		this.properties = properties;
	}

	public static List<ChannelHandlerConfigEntity> fromChannelhandlerConfig(
			ChannelHandlerConfigList defaultChannelPipeline) {
		
		if (defaultChannelPipeline==null) return null;
		
		Collection<ChannelHandlerConfigEntity> coll = Collections2.transform(defaultChannelPipeline, CHC_TO_ENTITY_FUNCTION);
		List<ChannelHandlerConfigEntity> list = new ArrayList<ChannelHandlerConfigEntity>(coll);
		
		return list;
	}
	
	public ChannelHandlerConfig toChannelHandlerConfig() {

		Multimap<String, String> multiMap = ArrayListMultimap.create();
		
		for ( KeyValueEntity entry : properties) {
			multiMap.put(entry.getKey(), entry.getValue());
		}
		return new ChannelHandlerConfig(handlerName, instanceName, multiMap);
	}
}
