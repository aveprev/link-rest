package com.nhl.link.rest.provider;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import com.nhl.link.rest.DataResponse;

/**
 * A JAX RS {@link DynamicFeature} that registers {@link ResponseStatusFilter}
 * for resource methods returning DataResponse.
 * 
 * @since 1.1
 */
public class ResponseStatusDynamicFeature implements DynamicFeature {

	@Override
	public void configure(ResourceInfo resourceInfo, FeatureContext context) {

		// ignore any other method signatures - in those cases we assume
		// response status is set by the user on his own..
		if (DataResponse.class.equals(resourceInfo.getResourceMethod().getReturnType())) {
			context.register(ResponseStatusFilter.class);
		}
	}

}
