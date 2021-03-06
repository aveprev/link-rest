package com.nhl.link.rest.runtime.processor.update;

import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;

import com.nhl.link.rest.EntityParent;
import com.nhl.link.rest.EntityUpdate;
import com.nhl.link.rest.LinkRestException;
import com.nhl.link.rest.UpdateResponse;
import com.nhl.link.rest.meta.LrEntity;
import com.nhl.link.rest.meta.LrPersistentAttribute;
import com.nhl.link.rest.meta.LrPersistentRelationship;
import com.nhl.link.rest.meta.LrRelationship;
import com.nhl.link.rest.parser.converter.Normalizer;
import com.nhl.link.rest.processor.ProcessingStage;
import com.nhl.link.rest.processor.Processor;
import com.nhl.link.rest.runtime.constraints.IConstraintsHandler;
import com.nhl.link.rest.runtime.encoder.IEncoderService;
import com.nhl.link.rest.runtime.meta.IMetadataService;

public class UpdateApplyServerParamsStage<T> extends ProcessingStage<UpdateContext<T>, T> {

	private IEncoderService encoderService;
	private IConstraintsHandler constraintsHandler;
	private IMetadataService metadataService;

	public UpdateApplyServerParamsStage(Processor<UpdateContext<T>, ? super T> next, IEncoderService encoderService,
			IConstraintsHandler constraintsHandler, IMetadataService metadataService) {
		super(next);
		this.encoderService = encoderService;
		this.constraintsHandler = constraintsHandler;
		this.metadataService = metadataService;
	}

	@Override
	protected void doExecute(UpdateContext<T> context) {

		UpdateResponse<T> response = context.getResponse();
		response.parent(context.getParent());

		if (context.isIncludingDataInResponse()) {
			response.includeData();
		} else {
			response.excludeData();
		}

		processExplicitId(context);
		processParentId(context);

		constraintsHandler.constrainUpdate(response, context.getWriteConstraints());

		// apply read constraints (TODO: should we only care about response
		// constraints after the commit?)
		constraintsHandler.constrainResponse(response, null, context.getReadConstraints());

		// TODO: we don't need encoder if includeData=false... should we
		// conditionally skip this step?
		response.withEncoder(encoderService.makeEncoder(response));
	}

	private void processExplicitId(UpdateContext<T> context) {

		if (context.getId() != null) {

			// id was specified explicitly ... this means a few things:
			// * we expect zero or one object in the body
			// * if zero, create an empty update that will be attached to the
			// ID.
			// * if more than one - throw...

			if (context.getResponse().getUpdates().isEmpty()) {
				context.getResponse().getUpdates().add(new EntityUpdate());
			}

			LrEntity<T> entity = context.getResponse().getEntity().getLrEntity();

			LrPersistentAttribute pk = (LrPersistentAttribute) entity.getSingleId();

			EntityUpdate u = context.getResponse().getFirst();
			u.getOrCreateId().put(pk.getDbAttribute().getName(),
					Normalizer.normalize(context.getId(), pk.getJavaType()));
			u.setExplicitId();
		}
	}

	private void processParentId(UpdateContext<T> context) {

		EntityParent<?> parent = context.getParent();

		if (parent != null && parent.getId() != null) {
			ObjRelationship fromParent = relationshipFromParent(context);

			if (fromParent != null && fromParent.isToDependentEntity()) {
				List<DbRelationship> dbRelationships = fromParent.getDbRelationships();

				DbRelationship last = dbRelationships.get(dbRelationships.size() - 1);

				if (last.getJoins().size() != 1) {
					throw new LinkRestException(Status.BAD_REQUEST,
							"Multi-join relationship propagation is not supported yet: "
									+ context.getResponse().getEntity().getLrEntity().getName());
				}

				String parentIdKey = last.getJoins().get(0).getTargetName();
				for (EntityUpdate u : context.getResponse().getUpdates()) {
					u.getOrCreateId().put(parentIdKey, parent.getId());
				}
			}
		}
	}

	private ObjRelationship relationshipFromParent(UpdateContext<?> context) {

		EntityParent<?> parent = context.getParent();

		if (parent == null) {
			return null;
		}

		LrRelationship r = metadataService.getLrRelationship(parent);
		if (r instanceof LrPersistentRelationship) {
			return ((LrPersistentRelationship) r).getObjRelationship();
		}

		return null;
	}

}
