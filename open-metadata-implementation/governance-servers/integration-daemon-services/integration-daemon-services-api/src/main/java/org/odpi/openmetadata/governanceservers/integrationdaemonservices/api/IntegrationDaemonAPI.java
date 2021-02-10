/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.governanceservers.integrationdaemonservices.api;

import org.odpi.openmetadata.commonservices.ffdc.rest.PropertiesResponse;
import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.governanceservers.integrationdaemonservices.properties.IntegrationServiceSummary;

import java.util.List;
import java.util.Map;

/**
 * IntegrationDaemonAPI is the interface to control and monitor an integration daemon.  The integration daemon is an OMAG Server.
 * It runs one-to-many integration services that in turn manage one-to-many integration connectors.  Each integration service
 * focuses on a particular type of third party technology and is paired with an appropriate OMAS.
 *
 * The refresh commands are used to instruct the connectors running in the integration daemon to verify the consistency
 * of the metadata in the third party technology against the values in open metadata.  All connectors are requested
 * to refresh when the integration daemon first starts.  Then refresh is called on the schedule defined in the configuration
 * and lastly as a result of calls to this API.
 */
public interface IntegrationDaemonAPI
{
    /**
     * Retrieve the configuration properties of the named connector.
     *
     * @param userId calling user
     * @param serviceURLMarker integration service identifier
     * @param connectorName name of a requested connector
     *
     * @return property map
     *
     * @throws InvalidParameterException the connector name is not recognized
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException there was a problem detected by the integration daemon
     */
    Map<String, Object> getConfigurationProperties(String userId,
                                                   String serviceURLMarker,
                                                   String connectorName) throws InvalidParameterException,
                                                                                UserNotAuthorizedException,
                                                                                PropertyServerException;

    /**
     * Update the configuration properties of the connectors, or specific connector if a connector name is supplied.
     *
     * @param userId calling user
     * @param serviceURLMarker integration service identifier
     * @param connectorName name of a specific connector or null for all connectors
     * @param isMergeUpdate should the properties be merged into the existing properties or replace them
     * @param configurationProperties new configuration properties
     *
     * @throws InvalidParameterException the connector name is not recognized
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException there was a problem detected by the integration daemon
     */
    void updateConfigurationProperties(String              userId,
                                       String              serviceURLMarker,
                                       String              connectorName,
                                       boolean             isMergeUpdate,
                                       Map<String, Object> configurationProperties) throws InvalidParameterException,
                                                                                           UserNotAuthorizedException,
                                                                                           PropertyServerException;


    /**
     * Refresh all connectors running in the integration daemon, regardless of the integration service they belong to.
     *
     * @param userId calling user
     *
     * @throws InvalidParameterException one of the parameters is null or invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException there was a problem detected by the integration daemon
     */
    void refreshAllServices(String userId) throws InvalidParameterException,
                                                  UserNotAuthorizedException,
                                                  PropertyServerException;



    /**
     * Refresh the requested connectors running in the requested integration service.
     *
     * @param userId calling user
     * @param serviceURLMarker integration service identifier
     * @param connectorName optional name of the connector to target - if no connector name is specified, all
     *                      connectors managed by this integration service are refreshed.
     *
     * @throws InvalidParameterException one of the parameters is null or invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException there was a problem detected by the integration daemon
     */
    void refreshService(String userId,
                        String serviceURLMarker,
                        String connectorName) throws InvalidParameterException,
                                                     UserNotAuthorizedException,
                                                     PropertyServerException;


    /**
     * Request that the integration service shutdown and recreate its integration connectors.  If a connector name
     * is provided, only that connector is restarted.
     *
     * @param userId calling user
     * @param serviceURLMarker integration service identifier
     * @param connectorName optional name of the connector to target - if no connector name is specified, all
     *                      connectors managed by this integration service are restarted.
     *
     * @throws InvalidParameterException one of the parameters is null or invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException there was a problem detected by the integration daemon
     */
    void restartService(String userId,
                        String serviceURLMarker,
                        String connectorName) throws InvalidParameterException,
                                                     UserNotAuthorizedException,
                                                     PropertyServerException;


    /**
     * Return a summary of each of the integration services' status.
     *
     * @param userId calling user
     *
     * @return list of statuses - on for each assigned integration services or
     *
     * @throws InvalidParameterException one of the parameters is null or invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException there was a problem detected by the integration daemon
     */
    List<IntegrationServiceSummary> getIntegrationDaemonStatus(String   userId) throws InvalidParameterException,
                                                                                       UserNotAuthorizedException,
                                                                                       PropertyServerException;
}
