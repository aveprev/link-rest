package com.nhl.link.rest.runtime.encoder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.nhl.link.rest.DataResponse;
import com.nhl.link.rest.ResourceEntity;
import com.nhl.link.rest.encoder.Encoder;
import com.nhl.link.rest.encoder.EncoderFilter;
import com.nhl.link.rest.it.fixture.pojo.model.P1;
import com.nhl.link.rest.it.fixture.pojo.model.P6;
import com.nhl.link.rest.meta.DefaultLrAttribute;
import com.nhl.link.rest.meta.LrEntity;
import com.nhl.link.rest.meta.LrEntityBuilder;
import com.nhl.link.rest.runtime.jackson.JacksonService;
import com.nhl.link.rest.runtime.semantics.RelationshipMapper;

public class EncoderService_Pojo_Test {

	private EncoderService encoderService;
	private List<EncoderFilter> filters;

	@Before
	public void setUp() {

		this.filters = new ArrayList<>();

		IAttributeEncoderFactory attributeEncoderFactory = new AttributeEncoderFactory();
		IStringConverterFactory stringConverterFactory = mock(IStringConverterFactory.class);

		this.encoderService = new EncoderService(this.filters, attributeEncoderFactory, stringConverterFactory,
				new RelationshipMapper());

	}

	@Test
	public void testEncode_SimplePojo_noId() throws IOException {
		LrEntity<P1> p1lre = LrEntityBuilder.build(P1.class);
		ResourceEntity<P1> descriptor = new ResourceEntity<P1>(p1lre);
		descriptor.getAttributes().put("name", new DefaultLrAttribute("name", String.class.getName()));

		DataResponse<P1> builder = DataResponse.forType(P1.class).resourceEntity(descriptor);

		P1 p1 = new P1();
		p1.setName("XYZ");
		assertEquals("[{\"name\":\"XYZ\"}]", toJson(p1, builder));
	}

	@Test
	public void testEncode_SimplePojo_Id() throws IOException {

		P6 p6 = new P6();
		p6.setStringId("myid");
		p6.setIntProp(4);

		LrEntity<P6> p6lre = LrEntityBuilder.builder(P6.class).build();
		ResourceEntity<P6> descriptor = new ResourceEntity<P6>(p6lre);
		descriptor.getAttributes().put("intProp", new DefaultLrAttribute("intProp", Integer.class.getName()));
		descriptor.includeId();
		DataResponse<P6> builder = DataResponse.forObjects(Collections.singletonList(p6)).resourceEntity(descriptor);

		assertEquals("[{\"id\":\"myid\",\"intProp\":4}]", toJson(p6, builder));
	}

	private String toJson(Object object, DataResponse<?> builder) throws IOException {

		Encoder encoder = encoderService.makeEncoder(builder);

		// wrap in collection... root encoder expects a list...
		object = Collections.singletonList(object);

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (JsonGenerator generator = new JacksonService().getJsonFactory().createGenerator(out, JsonEncoding.UTF8)) {
			encoder.encode(null, object, generator);
		}

		return new String(out.toByteArray(), "UTF-8");
	}

}
