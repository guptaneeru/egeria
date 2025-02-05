/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.dataengine.server.handlers;

import org.apache.commons.collections4.CollectionUtils;
import org.odpi.openmetadata.accessservices.dataengine.ffdc.DataEngineErrorCode;
import org.odpi.openmetadata.accessservices.dataengine.model.Attribute;
import org.odpi.openmetadata.accessservices.dataengine.model.DeleteSemantic;
import org.odpi.openmetadata.accessservices.dataengine.model.SchemaType;
import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
import org.odpi.openmetadata.commonservices.generichandlers.SchemaAttributeBuilder;
import org.odpi.openmetadata.commonservices.generichandlers.SchemaAttributeHandler;
import org.odpi.openmetadata.commonservices.generichandlers.SchemaTypeBuilder;
import org.odpi.openmetadata.commonservices.generichandlers.SchemaTypeHandler;
import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryHandler;
import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetailDifferences;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceHeader;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.ASSET_TO_SCHEMA_TYPE_TYPE_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.DISPLAY_NAME_PROPERTY_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.GUID_PROPERTY_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.LINEAGE_MAPPING_TYPE_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.REFERENCEABLE_TYPE_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.SCHEMA_ATTRIBUTE_TYPE_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.SCHEMA_TYPE_TYPE_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TABULAR_COLUMN_TYPE_GUID;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TABULAR_COLUMN_TYPE_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TABULAR_SCHEMA_TYPE_TYPE_GUID;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TABULAR_SCHEMA_TYPE_TYPE_NAME;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TYPE_TO_ATTRIBUTE_RELATIONSHIP_TYPE_GUID;
import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TYPE_TO_ATTRIBUTE_RELATIONSHIP_TYPE_NAME;

/**
 * DataEngineSchemaTypeHandler manages schema types objects from the property server. It runs server-side in the
 * DataEngine OMAS and creates and retrieves schema type entities through the OMRSRepositoryConnector.
 */
public class DataEngineSchemaTypeHandler {
    public static final String SCHEMA_TYPE_GUID_PARAMETER_NAME = "schemaTypeGUID";
    private final String serviceName;
    private final String serverName;
    private final RepositoryHandler repositoryHandler;
    private final OMRSRepositoryHelper repositoryHelper;
    private final InvalidParameterHandler invalidParameterHandler;
    private final SchemaTypeHandler<SchemaType> schemaTypeHandler;
    private final SchemaAttributeHandler<Attribute, SchemaType> schemaAttributeHandler;
    private final DataEngineRegistrationHandler dataEngineRegistrationHandler;
    private final DataEngineCommonHandler dataEngineCommonHandler;

    /**
     * Construct the handler information needed to interact with the repository services
     *
     * @param serviceName                   name of this service
     * @param serverName                    name of the local server
     * @param invalidParameterHandler       handler for managing parameter errors
     * @param repositoryHandler             manages calls to the repository services
     * @param repositoryHelper              provides utilities for manipulating the repository services objects
     * @param schemaTypeHandler             handler for managing schema elements in the metadata repositories
     * @param dataEngineRegistrationHandler provides calls for retrieving external data engine guid
     * @param dataEngineCommonHandler       provides utilities for manipulating entities
     * @param schemaAttributeHandler        handler for managing schema attributes in the metadata repositories
     */
    public DataEngineSchemaTypeHandler(String serviceName, String serverName, InvalidParameterHandler invalidParameterHandler,
                                       RepositoryHandler repositoryHandler, OMRSRepositoryHelper repositoryHelper,
                                       SchemaTypeHandler<SchemaType> schemaTypeHandler,
                                       SchemaAttributeHandler<Attribute, SchemaType> schemaAttributeHandler,
                                       DataEngineRegistrationHandler dataEngineRegistrationHandler,
                                       DataEngineCommonHandler dataEngineCommonHandler) {
        this.serviceName = serviceName;
        this.serverName = serverName;
        this.invalidParameterHandler = invalidParameterHandler;
        this.repositoryHelper = repositoryHelper;
        this.repositoryHandler = repositoryHandler;
        this.schemaTypeHandler = schemaTypeHandler;
        this.schemaAttributeHandler = schemaAttributeHandler;
        this.dataEngineRegistrationHandler = dataEngineRegistrationHandler;
        this.dataEngineCommonHandler = dataEngineCommonHandler;
    }

    /**
     * Create the schema type entity, with the corresponding schema attributes and relationships if it doesn't exist or
     * updates the existing one.
     *
     * @param userId             the name of the calling user
     * @param schemaType         the schema type values
     * @param externalSourceName the unique name of the external source
     *
     * @return unique identifier of the schema type in the repository
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public String upsertSchemaType(String userId, SchemaType schemaType, String externalSourceName) throws InvalidParameterException,
                                                                                                           PropertyServerException,
                                                                                                           UserNotAuthorizedException {
        final String methodName = "upsertSchemaType";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(schemaType.getQualifiedName(), QUALIFIED_NAME_PROPERTY_NAME, methodName);
        invalidParameterHandler.validateName(schemaType.getDisplayName(), DISPLAY_NAME_PROPERTY_NAME, methodName);

        Optional<EntityDetail> originalSchemaTypeEntity = findSchemaTypeEntity(userId, schemaType.getQualifiedName());

        SchemaTypeBuilder schemaTypeBuilder = getSchemaTypeBuilder(schemaType);

        String externalSourceGUID = dataEngineRegistrationHandler.getExternalDataEngine(userId, externalSourceName);

        String schemaTypeGUID;
        if (originalSchemaTypeEntity.isEmpty()) {
            schemaTypeGUID = schemaTypeHandler.addSchemaType(userId, externalSourceGUID, externalSourceName, schemaTypeBuilder, methodName);
        } else {
            schemaTypeGUID = originalSchemaTypeEntity.get().getGUID();
            EntityDetail updatedSchemaTypeEntity = buildSchemaTypeEntityDetail(schemaTypeGUID, schemaType);
            EntityDetailDifferences entityDetailDifferences = repositoryHelper.getEntityDetailDifferences(originalSchemaTypeEntity.get(),
                    updatedSchemaTypeEntity, true);

            if (entityDetailDifferences.hasInstancePropertiesDifferences()) {
                schemaTypeHandler.updateSchemaType(userId, externalSourceGUID, externalSourceName, schemaTypeGUID, SCHEMA_TYPE_GUID_PARAMETER_NAME,
                        schemaTypeBuilder);
            }
        }

        upsertSchemaAttributes(userId, schemaType, schemaTypeGUID, externalSourceName);

        return schemaTypeGUID;
    }

    /**
     * Find out if the SchemaType object is already stored in the repository. It uses the fully qualified name to retrieve the entity
     *
     * @param userId        the name of the calling user
     * @param qualifiedName the qualifiedName name of the schema type to be searched
     *
     * @return optional with entity details if found, empty optional if not found
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public Optional<EntityDetail> findSchemaTypeEntity(String userId, String qualifiedName) throws UserNotAuthorizedException,
                                                                                                   PropertyServerException,
                                                                                                   InvalidParameterException {
        return dataEngineCommonHandler.findEntity(userId, qualifiedName, SCHEMA_TYPE_TYPE_NAME);
    }

    /**
     * Find out if the SchemaAttribute object is already stored in the repository. It uses the fully qualified name to retrieve the entity
     *
     * @param userId        the name of the calling user
     * @param qualifiedName the qualifiedName name of the process to be searched
     *
     * @return optional with entity details if found, empty optional if not found
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public Optional<EntityDetail> findSchemaAttributeEntity(String userId, String qualifiedName) throws UserNotAuthorizedException,
                                                                                                        PropertyServerException,
                                                                                                        InvalidParameterException {
        return dataEngineCommonHandler.findEntity(userId, qualifiedName, SCHEMA_ATTRIBUTE_TYPE_NAME);
    }

    /**
     * Create LineageMapping relationship between two entities
     *
     * @param userId              the name of the calling user
     * @param sourceQualifiedName the qualified name of the source entity
     * @param targetQualifiedName the qualified name of the target entity
     * @param externalSourceName  the unique name of the external source
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public void addLineageMappingRelationship(String userId, String sourceQualifiedName, String targetQualifiedName, String externalSourceName)
            throws InvalidParameterException, UserNotAuthorizedException, PropertyServerException {
        final String methodName = "addLineageMappingRelationship";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(sourceQualifiedName, QUALIFIED_NAME_PROPERTY_NAME, methodName);
        invalidParameterHandler.validateName(targetQualifiedName, QUALIFIED_NAME_PROPERTY_NAME, methodName);

        Optional<EntityDetail> sourceEntity = getLineageMappingEntity(userId, sourceQualifiedName, methodName);
        Optional<EntityDetail> targetEntity = getLineageMappingEntity(userId, targetQualifiedName, methodName);

        if (sourceEntity.isEmpty()) {
            dataEngineCommonHandler.throwInvalidParameterException(DataEngineErrorCode.REFERENCEABLE_NOT_FOUND, methodName,
                    sourceQualifiedName);
            return;
        }
        if (targetEntity.isEmpty()) {
            dataEngineCommonHandler.throwInvalidParameterException(DataEngineErrorCode.REFERENCEABLE_NOT_FOUND, methodName,
                    targetQualifiedName);
            return;
        }
        dataEngineCommonHandler.upsertExternalRelationship(userId, sourceEntity.get().getGUID(), targetEntity.get().getGUID(),
                LINEAGE_MAPPING_TYPE_NAME, sourceEntity.get().getType().getTypeDefName(), externalSourceName, null);

    }

    /**
     * Returns the entity used to create the lineage mapping. It the entity is of type TabularSchemaType, then it will return the attached Asset
     *
     * @param userId        the name of the calling user
     * @param qualifiedName the qualified name of the entity
     * @param methodName    the method name
     *
     * @return An optional containing the entity for which to create the lineage mapping, or an empty optional
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    private Optional<EntityDetail> getLineageMappingEntity(String userId, String qualifiedName, String methodName) throws UserNotAuthorizedException,
                                                                                                                          PropertyServerException,
                                                                                                                          InvalidParameterException {
        Optional<EntityDetail> referenceableEntity = dataEngineCommonHandler.findEntity(userId, qualifiedName, REFERENCEABLE_TYPE_NAME);
        if (referenceableEntity.isEmpty()) {
            return Optional.empty();
        }

        EntityDetail entityDetail = referenceableEntity.get();
        if (TABULAR_SCHEMA_TYPE_TYPE_NAME.equalsIgnoreCase(entityDetail.getType().getTypeDefName())) {
            Optional<EntityDetail> assetEntity = dataEngineCommonHandler.getEntityForRelationship(userId, entityDetail.getGUID(),
                    ASSET_TO_SCHEMA_TYPE_TYPE_NAME, TABULAR_SCHEMA_TYPE_TYPE_NAME);
            if (assetEntity.isPresent()) {
                entityDetail = assetEntity.get();
            }
        }
        return Optional.of(entityDetail);
    }

    /**
     * Remove the schema type with the associated schema attributes
     *
     * @param userId             the name of the calling user
     * @param schemaTypeGUID     the unique identifier of the schema type
     * @param externalSourceName the external data engine
     * @param deleteSemantic     the delete semantic
     *
     * @throws InvalidParameterException     the bean properties are invalid
     * @throws UserNotAuthorizedException    user not authorized to issue this request
     * @throws PropertyServerException       problem accessing the property server
     * @throws FunctionNotSupportedException the repository does not support this call.
     */
    public void removeSchemaType(String userId, String schemaTypeGUID, String externalSourceName, DeleteSemantic deleteSemantic) throws
                                                                                                                                 InvalidParameterException,
                                                                                                                                 PropertyServerException,
                                                                                                                                 UserNotAuthorizedException,
                                                                                                                                 FunctionNotSupportedException {
        final String methodName = "removeSchemaType";
        dataEngineCommonHandler.validateDeleteSemantic(deleteSemantic, methodName);
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(schemaTypeGUID, GUID_PROPERTY_NAME, methodName);

        // remove the tabular columns manually, because schemaTypeHandler.removeSchemaType does not remove the columns
        Set<String> schemaAttributeGUIDs = getSchemaAttributesForSchemaType(userId, schemaTypeGUID);
        for (String schemaAttributeGUID : schemaAttributeGUIDs) {
            dataEngineCommonHandler.removeEntity(userId, schemaAttributeGUID, TABULAR_COLUMN_TYPE_NAME, externalSourceName);
        }
        dataEngineCommonHandler.removeEntity(userId, schemaTypeGUID, TABULAR_SCHEMA_TYPE_TYPE_NAME, externalSourceName);
    }

    private Set<String> getSchemaAttributesForSchemaType(String userId, String schemaTypeGUID) throws UserNotAuthorizedException,
                                                                                                      PropertyServerException,
                                                                                                      InvalidParameterException {
        Set<EntityDetail> entities = dataEngineCommonHandler.getEntitiesForRelationship(userId, schemaTypeGUID,
                TYPE_TO_ATTRIBUTE_RELATIONSHIP_TYPE_NAME, SCHEMA_TYPE_TYPE_NAME);

        if (CollectionUtils.isEmpty(entities)) {
            return new HashSet<>();
        }

        return entities.parallelStream().map(InstanceHeader::getGUID).collect(Collectors.toSet());
    }

    private void upsertSchemaAttributes(String userId, SchemaType schemaType, String schemaTypeGUID, String externalSourceName) throws
                                                                                                                                InvalidParameterException,
                                                                                                                                PropertyServerException,
                                                                                                                                UserNotAuthorizedException {

        String methodName = "upsertSchemaAttributes";
        for (Attribute tabularColumn : schemaType.getAttributeList()) {

            Optional<EntityDetail> schemaAttributeEntity = findSchemaAttributeEntity(userId, tabularColumn.getQualifiedName());

            if (schemaAttributeEntity.isEmpty()) {
                createSchemaAttribute(userId, schemaType, schemaTypeGUID, tabularColumn, tabularColumn.getDataType(), externalSourceName);
            } else {
                String schemaAttributeGUID = schemaAttributeEntity.get().getGUID();
                EntityDetail updatedSchemaAttributeEntity = buildSchemaAttributeEntityDetail(schemaAttributeGUID, tabularColumn);
                EntityDetailDifferences entityDetailDifferences = repositoryHelper.getEntityDetailDifferences(schemaAttributeEntity.get(),
                        updatedSchemaAttributeEntity, true);

                if (entityDetailDifferences.hasInstancePropertiesDifferences()) {
                    String externalSourceGUID = dataEngineRegistrationHandler.getExternalDataEngine(userId, externalSourceName);
                    schemaAttributeHandler.updateSchemaAttribute(userId, externalSourceGUID, externalSourceName,
                            schemaAttributeGUID, getSchemaAttributeBuilder(tabularColumn).getInstanceProperties(methodName));
                }
            }
        }
    }


    private EntityDetail buildSchemaAttributeEntityDetail(String schemaAttributeGUID, Attribute attribute) throws InvalidParameterException {
        String methodName = "buildSchemaAttributeEntityDetail";
        SchemaAttributeBuilder schemaAttributeBuilder = getSchemaAttributeBuilder(attribute);

        return dataEngineCommonHandler.buildEntityDetail(schemaAttributeGUID, schemaAttributeBuilder.getInstanceProperties(methodName));
    }

    SchemaAttributeBuilder getSchemaAttributeBuilder(Attribute attribute) {
        HashMap<String, String> additionalProperties = new HashMap<>();
        return new SchemaAttributeBuilder(attribute.getQualifiedName(), attribute.getDisplayName(), attribute.getDescription(),
                attribute.getPosition(), attribute.getMinCardinality(), attribute.getMaxCardinality(), attribute.getIsDeprecated(),
                attribute.getDefaultValueOverride(), attribute.getAllowsDuplicateValues(), attribute.getOrderedValues(),
                dataEngineCommonHandler.getSortOrder(attribute), attribute.getMinimumLength(), attribute.getLength(), attribute.getPrecision(),
                attribute.getIsNullable(), attribute.getNativeClass(), attribute.getAliases(), additionalProperties,
                attribute.getTypeGuid() != null ? attribute.getTypeGuid() : TABULAR_COLUMN_TYPE_GUID,
                attribute.getTypeName() != null ? attribute.getTypeName() : TABULAR_COLUMN_TYPE_NAME,
                null, repositoryHelper, serviceName, serverName);
    }

    private void createSchemaAttribute(String userId, SchemaType schemaType, String schemaTypeGUID, Attribute attribute,
                                       String dataType, String externalSourceName) throws InvalidParameterException, UserNotAuthorizedException,
                                                                                          PropertyServerException {
        final String methodName = "createSchemaAttribute";
        SchemaAttributeBuilder schemaAttributeBuilder = getSchemaAttributeBuilder(attribute);
        SchemaTypeBuilder schemaTypeBuilder = getSchemaTypeBuilder(schemaType);
        schemaTypeBuilder.setDataType(dataType);
        schemaAttributeBuilder.setSchemaType(userId, schemaTypeBuilder, methodName);
        final String schemaTypeGUIDParameterName = "schemaTypeGUID";
        final String qualifiedNameParameterName = "schemaAttribute.getQualifiedName()";

        String externalSourceGUID = dataEngineRegistrationHandler.getExternalDataEngine(userId, externalSourceName);

        schemaAttributeHandler.createNestedSchemaAttribute(userId, externalSourceGUID,
                externalSourceName, schemaTypeGUID, schemaTypeGUIDParameterName, TABULAR_SCHEMA_TYPE_TYPE_NAME,
                TYPE_TO_ATTRIBUTE_RELATIONSHIP_TYPE_GUID, TYPE_TO_ATTRIBUTE_RELATIONSHIP_TYPE_NAME,
                attribute.getQualifiedName(), qualifiedNameParameterName, schemaAttributeBuilder, methodName);
    }

    private EntityDetail buildSchemaTypeEntityDetail(String schemaTypeGUID, SchemaType schemaType) throws InvalidParameterException {
        String methodName = "buildSchemaTypeEntityDetail";

        SchemaTypeBuilder schemaTypeBuilder = getSchemaTypeBuilder(schemaType);
        return dataEngineCommonHandler.buildEntityDetail(schemaTypeGUID, schemaTypeBuilder.getInstanceProperties(methodName));
    }

    SchemaTypeBuilder getSchemaTypeBuilder(SchemaType schemaType) {
        return new SchemaTypeBuilder(schemaType.getQualifiedName(), schemaType.getDisplayName(), null,
                schemaType.getVersionNumber(), false, schemaType.getAuthor(), schemaType.getUsage(),
                schemaType.getEncodingStandard(), null, null,
                TABULAR_SCHEMA_TYPE_TYPE_GUID, TABULAR_SCHEMA_TYPE_TYPE_NAME,
                null, repositoryHelper, serviceName, serverName);
    }
}