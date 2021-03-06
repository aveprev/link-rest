package com.nhl.link.rest.unit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.ObjEntity;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.nhl.link.rest.ResourceEntity;
import com.nhl.link.rest.meta.DefaultLrAttribute;
import com.nhl.link.rest.meta.LrDataMap;
import com.nhl.link.rest.meta.LrEntity;
import com.nhl.link.rest.meta.LrEntityOverlay;
import com.nhl.link.rest.meta.cayenne.CayenneAwareLrDataMap;
import com.nhl.link.rest.runtime.cayenne.ICayennePersister;
import com.nhl.link.rest.runtime.meta.IMetadataService;
import com.nhl.link.rest.runtime.meta.MetadataService;

/**
 * A superclass of Cayenne-aware test cases that do not need to access the DB,
 * but need to work with EntityResolver and higher levels of the stack.
 */
public class TestWithCayenneMapping {

	protected static ServerRuntime runtime;

	@BeforeClass
	public static void setUpClass() {

		Module module = new Module() {

			@Override
			public void configure(Binder binder) {
				DataSourceFactory dsf = mock(DataSourceFactory.class);
				binder.bind(DataSourceFactory.class).toInstance(dsf);
			}
		};
		runtime = new ServerRuntime("cayenne-linkrest-tests.xml", module);
	}

	@AfterClass
	public static void tearDownClass() {
		runtime.shutdown();
		runtime = null;
	}

	protected LrDataMap lrDataMap;
	protected ICayennePersister mockCayennePersister;
	protected IMetadataService metadataService;

	@Before
	public void initLrDataMap() {
		lrDataMap = new CayenneAwareLrDataMap(runtime.getChannel().getEntityResolver(),
				Collections.<LrEntity<?>> emptyList(), Collections.<String, LrEntityOverlay<?>> emptyMap());

		ObjectContext sharedContext = runtime.newContext();

		this.mockCayennePersister = mock(ICayennePersister.class);
		when(mockCayennePersister.entityResolver()).thenReturn(runtime.getChannel().getEntityResolver());
		when(mockCayennePersister.sharedContext()).thenReturn(sharedContext);
		when(mockCayennePersister.newContext()).thenReturn(runtime.newContext());

		this.metadataService = createMetadataService();
	}

	protected IMetadataService createMetadataService() {
		return new MetadataService(Collections.<LrEntity<?>> emptyList(),
				Collections.<String, LrEntityOverlay<?>> emptyMap(), mockCayennePersister);
	}

	protected <T> LrEntity<T> getLrEntity(Class<T> type) {
		return lrDataMap.getEntity(type);
	}

	protected ObjEntity getEntity(Class<?> type) {
		return runtime.getChannel().getEntityResolver().getObjEntity(type);
	}

	protected <T> ResourceEntity<T> getResourceEntity(Class<T> type) {
		return new ResourceEntity<>(getLrEntity(type));
	}

	protected <T> void appendAttribute(ResourceEntity<?> entity, Property<T> property, Class<T> type) {
		appendAttribute(entity, property.getName(), type);
	}

	protected void appendAttribute(ResourceEntity<?> entity, String name, Class<?> type) {
		entity.getAttributes().put(name, new DefaultLrAttribute(name, type.getName()));
	}

}
