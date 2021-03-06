package com.nhl.link.rest.it.fixture.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.Ordering;

import com.nhl.link.rest.processor.ProcessingStage;
import com.nhl.link.rest.processor.Processor;
import com.nhl.link.rest.runtime.constraints.IConstraintsHandler;
import com.nhl.link.rest.runtime.encoder.IEncoderService;
import com.nhl.link.rest.runtime.parser.IRequestParser;
import com.nhl.link.rest.runtime.processor.IProcessorFactory;
import com.nhl.link.rest.runtime.processor.select.ApplyRequestStage;
import com.nhl.link.rest.runtime.processor.select.ApplyServerParamsStage;
import com.nhl.link.rest.runtime.processor.select.SelectContext;
import com.nhl.link.rest.runtime.processor.select.SelectInitStage;

public class PojoProcessorFactory implements IProcessorFactory {

	private IEncoderService encoderService;
	private IRequestParser requestParser;
	private IConstraintsHandler constraintsHandler;
	private PojoDB db;

	public PojoProcessorFactory(@Inject IEncoderService encoderService, @Inject IRequestParser requestParser,
			@Inject IConstraintsHandler constraintsHandler) {

		this.encoderService = encoderService;
		this.requestParser = requestParser;
		this.constraintsHandler = constraintsHandler;

		this.db = JerseyTestOnPojo.pojoDB;
	}

	@Override
	public Map<Class<?>, Map<String, Processor<?, ?>>> processors() {
		Map<Class<?>, Map<String, Processor<?, ?>>> map = new HashMap<>();
		map.put(SelectContext.class, Collections.<String, Processor<?, ?>> singletonMap(null, createSelectProcessor()));
		return map;
	}

	protected Processor<SelectContext<Object>, Object> createSelectProcessor() {

		ProcessingStage<SelectContext<Object>, Object> stage4 = new PojoFetchStage<>(null);
		ProcessingStage<SelectContext<Object>, Object> stage3 = new ApplyServerParamsStage<>(stage4, encoderService,
				constraintsHandler);
		ProcessingStage<SelectContext<Object>, Object> stage2 = new ApplyRequestStage<>(stage3, requestParser);
		ProcessingStage<SelectContext<Object>, Object> stage1 = new SelectInitStage<>(stage2);

		return stage1;
	}

	class PojoFetchStage<T> extends ProcessingStage<SelectContext<T>, T> {

		public PojoFetchStage(Processor<SelectContext<T>, ? super T> next) {
			super(next);
		}

		@Override
		protected void doExecute(SelectContext<T> context) {
			findObjects(context);
		}

		protected void findObjects(SelectContext<T> context) {

			Map<Object, T> typeBucket = db.bucketForType(context.getType());
			if (context.isById()) {
				T object = typeBucket.get(context.getId());
				context.getResponse().withObjects(
						object != null ? Collections.<T> singletonList(object) : Collections.<T> emptyList());
				return;
			}

			// clone the list and then filter/sort it as needed
			List<T> list = new ArrayList<>(typeBucket.values());

			Expression filter = context.getResponse().getEntity().getQualifier();
			if (filter != null) {

				Iterator<T> it = list.iterator();
				while (it.hasNext()) {
					T t = it.next();
					if (!filter.match(t)) {
						it.remove();
					}
				}
			}

			for (Ordering o : context.getResponse().getEntity().getOrderings()) {
				o.orderList(list);
			}

			context.getResponse().withObjects(list);
		}

	}
}
