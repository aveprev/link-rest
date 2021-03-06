package com.nhl.link.rest.runtime.cayenne;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.di.Inject;

import com.nhl.link.rest.processor.ProcessingStage;
import com.nhl.link.rest.processor.Processor;
import com.nhl.link.rest.runtime.UpdateOperation;
import com.nhl.link.rest.runtime.cayenne.processor.CayenneContextInitStage;
import com.nhl.link.rest.runtime.cayenne.processor.CayenneCreateOrUpdateStage;
import com.nhl.link.rest.runtime.cayenne.processor.CayenneCreateStage;
import com.nhl.link.rest.runtime.cayenne.processor.CayenneDeleteStage;
import com.nhl.link.rest.runtime.cayenne.processor.CayenneFetchStage;
import com.nhl.link.rest.runtime.cayenne.processor.CayenneFullSyncStage;
import com.nhl.link.rest.runtime.cayenne.processor.CayenneUnrelateStage;
import com.nhl.link.rest.runtime.cayenne.processor.CayenneUpdatePostProcessStage;
import com.nhl.link.rest.runtime.cayenne.processor.CayenneUpdateStage;
import com.nhl.link.rest.runtime.constraints.IConstraintsHandler;
import com.nhl.link.rest.runtime.encoder.IEncoderService;
import com.nhl.link.rest.runtime.meta.IMetadataService;
import com.nhl.link.rest.runtime.parser.IRequestParser;
import com.nhl.link.rest.runtime.processor.IProcessorFactory;
import com.nhl.link.rest.runtime.processor.delete.DeleteContext;
import com.nhl.link.rest.runtime.processor.delete.DeleteInitStage;
import com.nhl.link.rest.runtime.processor.select.ApplyRequestStage;
import com.nhl.link.rest.runtime.processor.select.ApplyServerParamsStage;
import com.nhl.link.rest.runtime.processor.select.SelectContext;
import com.nhl.link.rest.runtime.processor.select.SelectInitStage;
import com.nhl.link.rest.runtime.processor.unrelate.UnrelateContext;
import com.nhl.link.rest.runtime.processor.unrelate.UnrelateInitStage;
import com.nhl.link.rest.runtime.processor.update.UpdateApplyRequestStage;
import com.nhl.link.rest.runtime.processor.update.UpdateApplyServerParamsStage;
import com.nhl.link.rest.runtime.processor.update.UpdateContext;
import com.nhl.link.rest.runtime.processor.update.UpdateInitStage;

/**
 * @since 1.16
 */
public class CayenneProcessorFactory implements IProcessorFactory {

	private IRequestParser requestParser;
	private IEncoderService encoderService;
	private ICayennePersister persister;
	private IConstraintsHandler constraintsHandler;
	private IMetadataService metadataService;

	public CayenneProcessorFactory(@Inject IRequestParser requestParser, @Inject IEncoderService encoderService,
			@Inject ICayennePersister persister, @Inject IConstraintsHandler constraintsHandler,
			@Inject IMetadataService metadataService) {
		this.requestParser = requestParser;
		this.encoderService = encoderService;
		this.persister = persister;
		this.constraintsHandler = constraintsHandler;
		this.metadataService = metadataService;
	}

	@Override
	public Map<Class<?>, Map<String, Processor<?, ?>>> processors() {
		Map<Class<?>, Map<String, Processor<?, ?>>> map = new HashMap<>();
		map.put(SelectContext.class, Collections.<String, Processor<?, ?>> singletonMap(null, createSelectProcessor()));
		map.put(DeleteContext.class, Collections.<String, Processor<?, ?>> singletonMap(null, createDeleteProcessor()));
		map.put(UnrelateContext.class,
				Collections.<String, Processor<?, ?>> singletonMap(null, createUnrelateProcessor()));
		map.put(UpdateContext.class, createUpdateProcessors());
		return map;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, Processor<?, ?>> createUpdateProcessors() {

		Map map = new HashMap<>();

		map.put(UpdateOperation.create.name(), createCreateProcessor());
		map.put(UpdateOperation.createOrUpdate.name(), createOrUpdateProcessor(false));
		map.put(UpdateOperation.update.name(), createUpdateProcessor());
		map.put(UpdateOperation.idempotentCreateOrUpdate.name(), createOrUpdateProcessor(true));
		map.put(UpdateOperation.idempotentFullSync.name(), createFullSyncProcessor(true));

		return map;
	}

	private Processor<UnrelateContext<Object>, Object> createUnrelateProcessor() {
		ProcessingStage<UnrelateContext<Object>, Object> stage2 = new CayenneUnrelateStage<>(null, metadataService);
		ProcessingStage<UnrelateContext<Object>, Object> stage1 = new UnrelateInitStage<>(stage2);
		ProcessingStage<UnrelateContext<Object>, Object> stage0 = new CayenneContextInitStage<>(stage1, persister);

		return stage0;
	}

	private Processor<DeleteContext<Object>, Object> createDeleteProcessor() {
		ProcessingStage<DeleteContext<Object>, Object> stage2 = new CayenneDeleteStage<>(null);
		ProcessingStage<DeleteContext<Object>, Object> stage1 = new DeleteInitStage<>(stage2);
		ProcessingStage<DeleteContext<Object>, Object> stage0 = new CayenneContextInitStage<>(stage1, persister);

		return stage0;
	}

	private Processor<SelectContext<Object>, Object> createSelectProcessor() {

		ProcessingStage<SelectContext<Object>, Object> stage3 = new CayenneFetchStage<>(null, persister);
		ProcessingStage<SelectContext<Object>, Object> stage2 = new ApplyServerParamsStage<>(stage3, encoderService,
				constraintsHandler);
		ProcessingStage<SelectContext<Object>, Object> stage1 = new ApplyRequestStage<>(stage2, requestParser);
		ProcessingStage<SelectContext<Object>, Object> stage0 = new SelectInitStage<>(stage1);

		return stage0;
	}

	private Processor<UpdateContext<DataObject>, DataObject> createCreateProcessor() {

		ProcessingStage<UpdateContext<DataObject>, DataObject> stage5 = new CayenneUpdatePostProcessStage<>(null,
				Status.CREATED);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage4 = new CayenneCreateStage<>(stage5);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage3 = new UpdateApplyServerParamsStage<>(stage4,
				encoderService, constraintsHandler, metadataService);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage2 = new UpdateApplyRequestStage<>(stage3,
				requestParser);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage1 = new UpdateInitStage<>(stage2);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage0 = new CayenneContextInitStage<>(stage1, persister);

		return stage0;
	}

	private Processor<UpdateContext<DataObject>, DataObject> createUpdateProcessor() {

		ProcessingStage<UpdateContext<DataObject>, DataObject> stage5 = new CayenneUpdatePostProcessStage<>(null,
				Status.OK);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage4 = new CayenneUpdateStage<>(stage5);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage3 = new UpdateApplyServerParamsStage<>(stage4,
				encoderService, constraintsHandler, metadataService);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage2 = new UpdateApplyRequestStage<>(stage3,
				requestParser);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage1 = new UpdateInitStage<>(stage2);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage0 = new CayenneContextInitStage<>(stage1, persister);

		return stage0;
	}

	private Processor<UpdateContext<DataObject>, DataObject> createOrUpdateProcessor(boolean idempotnent) {

		ProcessingStage<UpdateContext<DataObject>, DataObject> stage5 = new CayenneUpdatePostProcessStage<>(null,
				Status.OK);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage4 = new CayenneCreateOrUpdateStage<>(stage5,
				idempotnent);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage3 = new UpdateApplyServerParamsStage<>(stage4,
				encoderService, constraintsHandler, metadataService);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage2 = new UpdateApplyRequestStage<>(stage3,
				requestParser);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage1 = new UpdateInitStage<>(stage2);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage0 = new CayenneContextInitStage<>(stage1, persister);

		return stage0;
	}

	private Processor<UpdateContext<DataObject>, DataObject> createFullSyncProcessor(boolean idempotnent) {

		ProcessingStage<UpdateContext<DataObject>, DataObject> stage5 = new CayenneUpdatePostProcessStage<>(null,
				Status.OK);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage4 = new CayenneFullSyncStage<>(stage5, idempotnent);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage3 = new UpdateApplyServerParamsStage<>(stage4,
				encoderService, constraintsHandler, metadataService);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage2 = new UpdateApplyRequestStage<>(stage3,
				requestParser);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage1 = new UpdateInitStage<>(stage2);
		ProcessingStage<UpdateContext<DataObject>, DataObject> stage0 = new CayenneContextInitStage<>(stage1, persister);

		return stage0;
	}
}
