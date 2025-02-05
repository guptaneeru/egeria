/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */

package org.odpi.openmetadata.accessservices.analyticsmodeling.responses;


import java.util.Arrays;
import java.util.List;

import org.odpi.openmetadata.accessservices.analyticsmodeling.model.ResponseContainerModule;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Response for request of the Analytics Modeling module. 
 *
 */

public class ModuleResponse extends AnalyticsModelingOMASAPIResponse {

	private List<ResponseContainerModule> data;

	/**
	 * Set module definition.
	 * @param module definition.
	 */
	@JsonIgnore
	public void setModule(ResponseContainerModule module) {
		data = Arrays.asList(module);
    }

	/**
	 * Get module definition.
	 * @return module definition.
	 */
	@JsonIgnore
	public ResponseContainerModule getModule() {
		return (data == null || data.isEmpty()) ? null : data.get(0);
    }

}
